package net.maritimeconnectivity.serviceregistry.components.mms;

import com.google.protobuf.ByteString;
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
        return new OutgoingMmtpMessage(MmtpMessage.newBuilder()
                .setMsgType(MsgType.PROTOCOL_MESSAGE)
                .setUuid(UUID.randomUUID().toString())
                .setProtocolMessage(ProtocolMessage.newBuilder()
                        .setProtocolMsgType(ProtocolMessageType.CONNECT_MESSAGE)
                        .setConnectMessage(Connect.newBuilder())
                ).build());
    }

    @Override
    public OutgoingMmtpMessage createSendMessage(String subject, String sender, String body, Duration ttl) {
        long expires = 0;
        byte[] payload = body.getBytes();

        return new OutgoingMmtpMessage(MmtpMessage.newBuilder()
                .setMsgType(MsgType.PROTOCOL_MESSAGE)
                .setUuid(UUID.randomUUID().toString())
                .setProtocolMessage(ProtocolMessage.newBuilder()
                        .setProtocolMsgType(ProtocolMessageType.SEND_MESSAGE)
                        .setSendMessage(Send.newBuilder()
                                .setApplicationMessage(ApplicationMessage.newBuilder()
                                        .setHeader(ApplicationMessageHeader.newBuilder()
                                                .setExpires(expires)
                                                .setBodySizeNumBytes(payload.length)
                                                .setSubject(subject)
                                                .setSender(sender)
                                        )
                                        .setSignature(null) // TODO Replace with actual signing algorithm
                                        .setBody(ByteString.copyFrom(payload))
                                )
                        )
                ).build());
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
        return new OutgoingMmtpMessage(MmtpMessage.newBuilder()
                .setMsgType(MsgType.PROTOCOL_MESSAGE)
                .setUuid(UUID.randomUUID().toString())
                .setProtocolMessage(ProtocolMessage.newBuilder()
                        .setProtocolMsgType(ProtocolMessageType.DISCONNECT_MESSAGE)
                        .setDisconnectMessage(Disconnect.newBuilder())
                ).build());
    }




}
