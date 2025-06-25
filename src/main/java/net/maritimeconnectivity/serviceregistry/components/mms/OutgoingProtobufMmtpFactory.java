package net.maritimeconnectivity.serviceregistry.components.mms;

import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.mmtp.*;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.UUID;

@Component
@Slf4j
public class OutgoingProtobufMmtpFactory implements OutgoingMmtpFactory {

    @Override
    public OutgoingMmtpMessage createConnectMessage() {
        // Implementation for creating a connect message
        return null; // Replace with actual implementation
    }

    @Override
    public OutgoingMmtpMessage createSendMessage(String subject, String sender, String body, Duration ttl) {
        return null;
    }

    @Override
    public OutgoingMmtpMessage createReceiveMessage() {
        return new OutgoingMmtpMessage(MmtpMessage.newBuilder()
                .setMsgType(MsgType.PROTOCOL_MESSAGE)
                .setUuid(UUID.randomUUID().toString())
                .setProtocolMessage(ProtocolMessage.newBuilder()
                        .setProtocolMsgType(ProtocolMessageType.RECEIVE_MESSAGE)
                        .setReceiveMessage(Receive.newBuilder())
                ).build());
    }

    @Override
    public OutgoingMmtpMessage createDisconnectMessage() {
        // Implementation for creating a disconnect message
        return null; // Replace with actual implementation
    }




}
