package net.maritimeconnectivity.serviceregistry.components.mms;

import com.google.protobuf.InvalidProtocolBufferException;
import jakarta.annotation.PreDestroy;
import net.maritimeconnectivity.mmtp.*;
import net.maritimeconnectivity.serviceregistry.utils.KeyStoreUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.UUID;
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

    @Value("${info.mms.router.url}")
    private String routerUrl;
    private final KeyStoreUtil keystoreUtil;
    private final MmtpFactory mmtpFactory;

    private WebSocketSession webSocketSession;

    /**
     * Constructor for MmsEdgeRouter.
     *
     * @param keystoreUtil Utility for handling keystore operations.
     */  @Autowired
    public MmsEdgeRouter(KeyStoreUtil keystoreUtil, MmtpFactory mmtpFactory) {
         this.keystoreUtil = keystoreUtil;
         this.mmtpFactory = mmtpFactory;
    }

    @PreDestroy
    public void preDestroy() throws IOException, InterruptedException {
        //Handle closing of websocket and mmtp session somewhat gracefully
    }

    public void sendMessage(MmtpMessage mmtpMessage) throws IOException {
        byte[] bytes = mmtpMessage.toByteArray();
        webSocketSession.sendMessage(new BinaryMessage(bytes));

        // TODO: Possibly keep track of sent messages and UUIDs in order to be able to report a progress to GMSP

    }

    //Send an MMTP receive to the Router
    private void receive() throws IOException {
        MmtpMessage receiveMessage = this.mmtpFactory.createReceiveMessage();
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
                // TODO: Attempt parsding of payload as JSON object according to MSR open API

                // TODO: Call proper API

            } else {
                log.error("Cannot handle message type: {}", type);
            }

        // Case: Response from Router when sending global search request to the MMS Network
        } else if (msg.hasResponseMessage()) {
            ResponseMessage resp = msg.getResponseMessage();
            if (resp.getResponse() != ResponseEnum.GOOD) {
                String respToUuid = resp.getResponseToUuid();
                String reason = resp.getReasonText();
                log.error("Error response from MMS Router for UUID {}: Code: {}: {}", respToUuid, resp.getResponse(), reason);

                // TODO: Possible action to re-transmit the message or notify the user
            } else {
                log.info("Message {} successfully sent to MMS Router", resp.getResponseToUuid());
            }
        }
    }

    private void connect() {
         try {
             connectWebSocket();
             connectMmtp();
         } catch (Exception e) {
             log.error("Error connecting to MMS Router", e);
         }
    }


    private void connectMmtp() throws IOException {
         mmtpFactory.createConnectMessage();
         sendMessage(mmtpFactory.createConnectMessage());
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
        URI uri = new URI(routerUrl);
        webSocketSession = webSocketClient.execute(new MMSWebsocketHandler(this), null, uri).get();
        log.info("Connected to MMS router {}", routerUrl);
    }


    private byte[] generateSignature(String subject, long expires, String ownMrn, byte []body) {
        return new byte[0];
    }





    private class MMSWebsocketHandler extends BinaryWebSocketHandler {

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