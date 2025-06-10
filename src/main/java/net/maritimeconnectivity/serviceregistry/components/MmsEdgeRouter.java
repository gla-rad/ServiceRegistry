package net.maritimeconnectivity.serviceregistry.components;

import jakarta.annotation.PreDestroy;
import net.maritimeconnectivity.mmtp.MmtpMessage;
import net.maritimeconnectivity.serviceregistry.utils.KeyStoreUtil;
import org.geolatte.geom.M;
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

    private WebSocketSession webSocketSession;

    /**
     * Constructor for MmsEdgeRouter.
     *
     * @param keystoreUtil Utility for handling keystore operations.
     */  @Autowired
    public MmsEdgeRouter(KeyStoreUtil keystoreUtil) {
        this.keystoreUtil = keystoreUtil;
    }

    @PreDestroy
    public void preDestroy() throws IOException, InterruptedException {
        //Handle closing of websocket and mmtp session somewhat gracefully
    }

    public void sendMessage(MmtpMessage mmtpMessage) {

    }

    //Handler triggered when a message is received from the WebSocket
    public void handleMessage() {

    }

    private void connectWebSocket () {

    }

    private byte[] generateSignature(String subject, long expires, String ownMrn, byte []body) {
        return new byte[0];
    }





    private class MMSWebsocketHandler extends BinaryWebSocketHandler {

        private final MmsEdgeRouter edgeRouterRef;

         //constreuctor
        public MMSWebsocketHandler(MmsEdgeRouter er) {
            this.edgeRouterRef = er;

        }

         @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        }

        @Override
        protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
            // Handle incoming binary messages
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        }

    }

}