package net.maritimeconnectivity.serviceregistry.components.mms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.maritimeconnectivity.mmtp.*;
import net.maritimeconnectivity.serviceregistry.components.Gmsp;
import net.maritimeconnectivity.serviceregistry.models.dto.mms.MmsSearchMessageDto;
import net.maritimeconnectivity.serviceregistry.utils.KeyStoreUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

/**
 * The MMs Edge Router Component
 *
 * This component is responsible for sending MMS messages to a designated MMS Router Network
 * as required by MSR Technical Specifcation Requirement 14.A.
 * The implementation contains the necessary subset of functionalities (described in RTCM 13900.0) for an edge router
 * to subscribe and send subject case messages to the MMS Router Network.
 */
@Component
@Slf4j
public class MmsEdgeRouter {

    public static final int RETRANSMISSION_NUM = 5;

    private volatile  boolean connected = false;
    private volatile boolean initialized = false;

    @Value("${info.mms.router.url}")
    private String routerUrl;
    private String ownMrn;
    private final KeyStoreUtil keystoreUtil;
    private final OutgoingMmtpFactory mmtpFactory;

    private WebSocketSession webSocketSession;
    private Gmsp gmsp;
    private HashMap<String, OutgoingMmtpMessage> msgBuffer = new HashMap<>();


    @Autowired //Necessary to avoid circular dependency as the Gmsp has The edgerouter constructor injected
    @Lazy
    public void setGmsp(Gmsp gmsp) {
        this.gmsp = gmsp;
    }

    /**
     * Constructor for MmsEdgeRouter.
     *
     * @param keystoreUtil Utility for handling keystore operations.
     */  @Autowired
    public MmsEdgeRouter(KeyStoreUtil keystoreUtil, OutgoingMmtpFactory mmtpFactory) {
        this.keystoreUtil = keystoreUtil;
        this.mmtpFactory = mmtpFactory;

        try {
            this.ownMrn = keystoreUtil.getOwnMrn();
        } catch (Exception e) {
            log.error("Error retrieving own MRN from keystore: {}", e.getMessage());
        }
        initialized = true;
    }

    @PostConstruct
    public void init() {
        try {
            this.ownMrn = keystoreUtil.getOwnMrn();
            log.info("Successfully retrieved own MRN: {}", ownMrn);
        } catch (Exception e) {
            log.error("Error retrieving own MRN from keystore", e);
        }

        log.info("Initializing MmsEdgeRouter with router URL: {}", routerUrl);
        this.initialized = true;

        try {
            connect();
            this.connected = true;
            log.info("Successfully connected to MMS Router");
        } catch (Exception e) {
            log.error("Error connecting to MMS Router", e);
            this.connected = false; //
        }
    }


    @PreDestroy
    public void preDestroy() throws IOException, InterruptedException {
         //Make sure we are disconnected
        if (webSocketSession.isOpen()) {
            OutgoingMmtpMessage disconnectMessage = mmtpFactory.createDisconnectMessage();
            sendMessage(disconnectMessage);
        }

        //Handle closing of websocket and mmtp session somewhat gracefully
    }

    public void sendMessage(OutgoingMmtpMessage msg) throws IOException {

        String uuid = msg.getMessage().getUuid();

        byte[] bytes = msg.getMessage().toByteArray();

        //Only add on first attempt to send
        if (!this.msgBuffer.containsKey(uuid) && msg.getMessage().getProtocolMessage().hasSendMessage())  {
            this.msgBuffer.put(uuid, msg);
        }
        webSocketSession.sendMessage(new BinaryMessage(bytes));

        log.info("Sent message with UUID: {} to MMS Router", uuid);
        msg.incrementSendAttempts();
        msg.updateTimestamp();
    }

    //Send an MMTP receive to the Router
    private void receive() throws IOException {
        OutgoingMmtpMessage receiveMessage = this.mmtpFactory.createReceiveMessage();
        sendMessage(receiveMessage);
    }

    // Will send a receive
    private void handleNotify() throws IOException {
        this.receive();
    }


    //Handler triggered when a message is received from the WebSocket
    public void handleMessage(MmtpMessage msg) {
        // Case: Incoming global search request
        if (msg.hasProtocolMessage()) {

            // Check if it is a Notify,
            var type = msg.getProtocolMessage().getProtocolMsgType();
            if (type == ProtocolMessageType.NOTIFY_MESSAGE) {
                try {
                    handleNotify();
                } catch (IOException e) {
                    log.error("Error pulling messages from router upon receiving a Notify", e);
                }
            } else if (type == ProtocolMessageType.SEND_MESSAGE) {
                byte[] body = msg.getProtocolMessage().getSendMessage().getApplicationMessage().getBody().toByteArray();

                try {
                    String json = new String(body);
                    MmsSearchMessageDto dto = gmsp.parseSearchDto(json);
                    gmsp.handleIncomingGlobalSearch(dto);

                } catch (JsonProcessingException e) {
                    log.error("Error parsing JSON from MMS Router: {}", e.getMessage());
                }
            } else {
                log.error("Cannot handle message type: {}", type);
            }

        // Case: Response from Router when sending global search request to the MMS Network or
        // response to a receive message
        } else if (msg.hasResponseMessage()) {
            String responseToUuid = msg.getResponseMessage().getResponseToUuid();
            OutgoingMmtpMessage bufferedMsg = this.msgBuffer.get(responseToUuid);

            ResponseMessage resp = msg.getResponseMessage();
            log.info("New response to UUID {}: Code: {}, Reason: {}", responseToUuid, resp.getResponse(), resp.getReasonText());
            if (resp.getResponse() != ResponseEnum.GOOD) {
                String reason = resp.getReasonText();

                if (bufferedMsg.getSendAttempts() < RETRANSMISSION_NUM) {
                    log.error("Error response from MMS Router for UUID {}: Code: {}: {}, Attempting Retransmit...", responseToUuid, resp.getResponse(), reason);
                    try {
                        this.sendMessage(bufferedMsg);
                    } catch (Exception e) {
                        log.error("Error sending retransmit.", e);
                    }
                } else {
                    log.error("Error response from MMS Router for UUID {}: Code {}: {}, Cannot re-transmit, Discarding", responseToUuid, resp.getResponse(), reason);
                }
            } else {
                if (this.msgBuffer.containsKey(responseToUuid)) {
                    log.info("ACK received from router: Message {} was successfully sent to the MMS Router", resp.getResponseToUuid());
                    gmsp.globalSearchRequestCallback(bufferedMsg.getGsrUuid());
                    this.msgBuffer.remove(responseToUuid);
                } else {
                    log.error("Received response to unknown message: {}", resp.getResponseToUuid());
                }
            }
        }
    }

    public boolean isBuffered(String uuid) {
         return this.msgBuffer.containsKey(uuid);
    }

    private void connect() throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, URISyntaxException, IOException, KeyStoreException, ExecutionException, InterruptedException, KeyManagementException {
         connectWebSocket();
         connectMmtp();
         this.connected = true;
    }

    private void connectMmtp() throws IOException {
         OutgoingMmtpMessage msg =  mmtpFactory.createConnectMessage(ownMrn);

         log.info("Own mrn in connect msg is : {}", msg.getMessage().getProtocolMessage().getConnectMessage().getOwnMrn());

         sendMessage(msg);
    }

    private void connectWebSocket () throws
            NoSuchAlgorithmException,
            URISyntaxException,
            ExecutionException,
            InterruptedException,
            CertificateException,
            UnrecoverableKeyException,
            IOException,
            KeyStoreException,
            KeyManagementException
    {

        //Setup ssl context
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keystoreUtil.getMmsKeystore(), keystoreUtil.getMmsKeystorePassword());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

        StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
        webSocketClient.setSslContext(sslContext);
        URI uri = new URI(routerUrl);
        webSocketSession = webSocketClient.execute(new MMSWebsocketHandler(this), null, uri).get();
        log.info("WS Connected to MMS router {}", routerUrl);
    }



    private static class MMSWebsocketHandler extends BinaryWebSocketHandler {

        private final MmsEdgeRouter edgeRouterRef;

        //constructor
        public MMSWebsocketHandler(MmsEdgeRouter er) {
            this.edgeRouterRef = er;

        }

         @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            log.info("WebSocket connection established with {}", session.getRemoteAddress());

        }

        // Handles an incoming binary message
        @Override
        protected void handleBinaryMessage(@NotNull WebSocketSession session, @NotNull BinaryMessage message) {
            try {
                MmtpMessage mmtpMsg = MmtpMessage.parseFrom(message.getPayload());
                this.edgeRouterRef.handleMessage(mmtpMsg);

            } catch (InvalidProtocolBufferException e) {
                log.error("Protobuf error when de-serializing, {}", e.getMessage());
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) {
            log.info("WebSocket connection closed with status: {}", status);
            log.info("Is session open? {}", session.isOpen());
            this.edgeRouterRef.webSocketSession = null;
        }
    }

}