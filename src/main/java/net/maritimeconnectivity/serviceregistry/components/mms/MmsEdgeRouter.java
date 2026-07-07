package net.maritimeconnectivity.serviceregistry.components.mms;

import com.google.protobuf.InvalidProtocolBufferException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import net.maritimeconnectivity.mmtp.*;
import net.maritimeconnectivity.serviceregistry.components.Gmsp;
import net.maritimeconnectivity.serviceregistry.models.dto.mms.MmsSearchMessageDto;
import net.maritimeconnectivity.serviceregistry.utils.KeyStoreUtil;
import net.maritimeconnectivity.serviceregistry.utils.ReconnectTokenUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
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
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;

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
@ConditionalOnProperty(value = "info.gmsp.enabled", havingValue = "true")
public class MmsEdgeRouter {

    private final TaskScheduler scheduler;
    private final Object reconnectLock = new Object();
    private volatile ScheduledFuture<?> reconnectTask;

    public static final int RETRANSMISSION_NUM = 5;

    @Getter
    private volatile  boolean connected = false;
    private volatile boolean initialized = false;

    @Value("${info.mms.router.url}")
    private String routerUrl;
    private String ownMrn;
    private final KeyStoreUtil keystoreUtil;
    private final OutgoingMmtpFactory mmtpFactory;

    private String awaitConnectResponseToUuid;


    private WebSocketSession webSocketSession;
    private Gmsp gmsp;

    // Use newKeySet from concurrentHashmap for thread safety
    private final Set<String> subscriptions;
    private final ConcurrentHashMap<String, OutgoingMmtpMessage> msgBuffer = new ConcurrentHashMap<>();


    @Autowired
    //Necessary to avoid circular dependency as the Gmsp has the edgerouter constructor injected
    //This is needed in order for the gmsp callbacks to work
    @Lazy
    public void setGmsp(Gmsp gmsp) {
        this.gmsp = gmsp;
    }

    /**
     * Constructor for MmsEdgeRouter.
     *
     * @param keystoreUtil Utility for handling keystore operations.
     */  @Autowired
    public MmsEdgeRouter(TaskScheduler taskScheduler, KeyStoreUtil keystoreUtil, OutgoingMmtpFactory mmtpFactory) {
        this.scheduler = taskScheduler;
        this.keystoreUtil = keystoreUtil;
        this.mmtpFactory = mmtpFactory;
        this.subscriptions = ConcurrentHashMap.newKeySet();

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
        } catch (Exception e) {
            log.error("Error retrieving own MRN from keystore", e);
        }

        log.debug("Initializing MmsEdgeRouter with router URL: {}", routerUrl);
        this.initialized = true;

        try {
            connect();
            log.info("MMS Edgerouter sucessfully setup");
        } catch (Exception e) {
            log.error("Error connecting to MMS Router", e);
            this.connected = false; //
            this.startReconnectLoop();
        }
    }


    @PreDestroy
    public void preDestroy() {
        try {
            ScheduledFuture<?> toCancel;
            synchronized (reconnectLock) {
                toCancel = reconnectTask;
                reconnectTask = null;
            }
            if (toCancel != null) toCancel.cancel(false);

            if (webSocketSession != null && webSocketSession.isOpen()) {
                OutgoingMmtpMessage disconnectMessage = mmtpFactory.createDisconnectMessage();
                sendMessage(disconnectMessage);
                webSocketSession.close();
            }
        } catch (Exception e) {
            log.error("Error during shutdown: {}", e.getMessage(), e);
        }
    }


    public void sendMessage(OutgoingMmtpMessage msg) throws IOException {
        if (webSocketSession == null || !this.webSocketSession.isOpen()) {
            throw new IOException("Web socket is closed");
        }

        String uuid = msg.getMessage().getUuid();

        byte[] bytes = msg.getMessage().toByteArray();

        //Only add on first attempt to send
        if (!this.msgBuffer.containsKey(uuid))  {
            this.msgBuffer.put(uuid, msg);
        }
        webSocketSession.sendMessage(new BinaryMessage(bytes));

        log.debug("Sent message with UUID: {} to MMS Router", uuid);
        msg.incrementSendAttempts();
        msg.updateTimestamp();
    }


    public void subscribe(OutgoingMmtpMessage msg) throws IOException {
        String subject = msg.getMessage().getProtocolMessage().getSubscribeMessage().getSubject();
        if (!this.subscriptions.contains(subject)) {
            this.subscriptions.add(subject);
            this.sendMessage(msg);
        } else {
            log.warn("Already subscribed to subject: {}", subject);
        }

    }

    public void unsubscribe(OutgoingMmtpMessage msg) throws IOException {
        String subject = msg.getMessage().getProtocolMessage().getUnsubscribeMessage().getSubject();
        if (this.subscriptions.contains(subject)) {
            this.subscriptions.remove(subject);
            this.sendMessage(msg);
        } else {
            log.warn("Not subscribed to subject: {}", subject);
        }
    }

    // Returns a copy of the current subscriptions
    public  Set<String> getSubscriptions() {
        return new HashSet<>(this.subscriptions);
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
                    log.debug("Received NOTIFY message from MMS Router: {} You have {} new messages", msg.getUuid(), msg.getProtocolMessage().getNotifyMessage().getMessageMetadataCount());
                    handleNotify();
                } catch (IOException e) {
                    log.error("Error pulling messages from router upon receiving a Notify", e);
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
            log.debug("Response message to UUID {}: Code: {}, Reason: {}", responseToUuid, resp.getResponse(), resp.getReasonText());
            if (resp.getResponse() != ResponseEnum.GOOD) {
                String reason = resp.getReasonText();

                if (bufferedMsg.getSendAttempts() < RETRANSMISSION_NUM && bufferedMsg.getMessage().getProtocolMessage().hasSendMessage()) {
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
                if (responseToUuid.equals(awaitConnectResponseToUuid)) {
                    log.info("Sucessfully Connected to MMS Router at {}", routerUrl);
                    synchronized (reconnectLock) {
                        this.connected = true;
                        reconnectLock.notifyAll();
                    }
                }

                if (this.msgBuffer.containsKey(responseToUuid)) {
                    log.debug("Response was expected for uuid {}", resp.getResponseToUuid());
                    gmsp.globalSearchRequestCallback(bufferedMsg.getGsrUuid());
                    this.msgBuffer.remove(responseToUuid);

                    // Possibly incoming GMSP search requests
                    List<MessageContent> content = msg.getResponseMessage().getMessageContentList();
                    for (MessageContent c : content) {
                        ApplicationMessage appMsg = c.getMsg();
                        if (appMsg.hasHeader()) {
                            String subject = appMsg.getHeader().getSubject();
                            log.debug("Message with subject: {}", subject);

                            if (this.subscriptions.contains(subject)) {
                                // Parse the content to a MmsSearchMessageDto
                                var rawContent = appMsg.getBody().toByteArray();

                                // Attempt to parse the content to a MmsSearchMessageDto
                                MmsSearchMessageDto msgDto = null;
                                try {
                                    msgDto = gmsp.mmsSearchMessageDTOfromJSON(new String(rawContent));
                                    gmsp.handleIncomingGlobalSearch(msgDto);
                                } catch (UnrecoverableKeyException | CertificateException | IOException |
                                         KeyStoreException | NoSuchAlgorithmException e) {
                                    log.error("Error parsing MmsSearchMessageDto from content: {}", e.getMessage());
                                }
                                return;
                            }
                        }
                    }
                    String rcToken = msg.getResponseMessage().getReconnectToken();
                    if (!rcToken.isBlank()) {
                        log.debug("Received reconnect token and writing to disk: {}", rcToken);
                        try {
                            ReconnectTokenUtil.writeRcToken(rcToken);
                        } catch (IOException e) {
                            log.error("Error writing reconnect token to disk: {}", e.getMessage());
                        }
                    }


                } else {
                    log.error("Received response to unknown message: {}", resp.getResponseToUuid());
                }
            }
        }
    }

    private void connect() throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, URISyntaxException, IOException, KeyStoreException, ExecutionException, InterruptedException, KeyManagementException {
         connectWebSocket();
         connectMmtp();
    }

    private void connectMmtp() throws IOException {
         OutgoingMmtpMessage msg = mmtpFactory.createConnectMessage(this.ownMrn);
         // Load rcToken if a such has been stored
//        try {
//            String rcToken = ReconnectTokenUtil.readRcToken();
//
//            msg = mmtpFactory.createConnectMessage(this.ownMrn, rcToken);
//            log.debug("Connect to mmtp using token: {}", msg.getMessage().getProtocolMessage().getConnectMessage().getReconnectToken());
//
//        } catch (IOException e) {
//            log.debug("No reconnect token available");
//        }

        log.debug("Own mrn in connect msg is : {}", msg.getMessage().getProtocolMessage().getConnectMessage().getOwnMrn());
        awaitConnectResponseToUuid = msg.getMessage().getUuid();

        sendMessage(msg);
    }

    private void startReconnectLoop() {
        synchronized (reconnectLock) {
            if (reconnectTask != null && !reconnectTask.isCancelled()) return;
            reconnectTask = scheduler.scheduleWithFixedDelay(() -> {
                if (connected) return;
                try {
                    connect();

                    // When connected becomes true

                    synchronized (reconnectLock) {
                        while (!connected) {
                            reconnectLock.wait(5000); // wake on notify or every 5s to re-check
                        }
                    }

                    log.debug("Initialize subs again", routerUrl);
                    this.gmsp.initializeSubscriptionsFromDb();

                    ScheduledFuture<?> toCancel;
                    synchronized (reconnectLock) {
                        toCancel = reconnectTask;
                        reconnectTask = null;
                    }
                    log.info("Sucessfully restored state with the to MMS Router.");
                    if (toCancel != null) toCancel.cancel(false);
                } catch (Exception e) {
                    log.debug("Reconnect failed: {}", e.getMessage());
                }
            }, Duration.ofSeconds(10));
        }


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

        //Check we have loaded cert
        if (keystoreUtil.getMmsKeystore().size() == 0) {
            log.warn("No certificates found in the MMS keystore. Check your configuration.");
        } else {
            log.debug("Loaded {} certificates from the MMS keystore", keystoreUtil.getMmsKeystore().size());
        }

        webSocketClient.setSslContext(sslContext);
        URI uri = new URI(routerUrl);
        webSocketSession = webSocketClient.execute(new MMSWebsocketHandler(this), null, uri).get();
        log.debug("WS Connected to MMS router {}", routerUrl);
    }



    private static class MMSWebsocketHandler extends BinaryWebSocketHandler {

        private final MmsEdgeRouter edgeRouterRef;

        //constructor
        public MMSWebsocketHandler(MmsEdgeRouter er) {
            this.edgeRouterRef = er;

        }

         @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            session.setBinaryMessageSizeLimit(100 * 1024 * 1024); // 100 MiB
            log.debug("WebSocket connection established with {}, buffer {}", session.getRemoteAddress(), session.getTextMessageSizeLimit());

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
            log.debug("WebSocket connection closed with status: {}", status);
            this.edgeRouterRef.webSocketSession = null;
            this.edgeRouterRef.connected = false;
            edgeRouterRef.subscriptions.clear();

            if (status.getCode() != CloseStatus.NORMAL.getCode()) {
                log.warn("WebSocket connection closed unexpectedly. Starting reconnect loop");
                this.edgeRouterRef.startReconnectLoop();
            } else  {
                log.debug("WebSocket connection closed standard code 1000 code {}", status.getCode());
            }

        }
    }

}
