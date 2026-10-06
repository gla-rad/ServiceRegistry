package net.maritimeconnectivity.serviceregistry.components.mms;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.mmtp.*;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class OutgoingProtobufMmtpFactory implements OutgoingMmtpFactory {

    @Override
    public OutgoingMmtpMessage createConnectMessage(String ownMrn) {
        return new OutgoingMmtpMessage(MmtpMessage.newBuilder()
                .setMsgType(MsgType.PROTOCOL_MESSAGE)
                .setUuid(UUID.randomUUID().toString())
                .setProtocolMessage(ProtocolMessage.newBuilder()
                        .setProtocolMsgType(ProtocolMessageType.CONNECT_MESSAGE)
                        .setConnectMessage(Connect.newBuilder()
                                .setOwnMrn(ownMrn))
                ).build());
    }

    public OutgoingMmtpMessage createConnectMessage(String ownMrn, String reconnectToken) {
        return new OutgoingMmtpMessage(MmtpMessage.newBuilder()
                .setMsgType(MsgType.PROTOCOL_MESSAGE)
                .setUuid(UUID.randomUUID().toString())
                .setProtocolMessage(ProtocolMessage.newBuilder()
                        .setProtocolMsgType(ProtocolMessageType.CONNECT_MESSAGE)
                        .setConnectMessage(Connect.newBuilder()
                                .setOwnMrn(ownMrn)
                                .setReconnectToken(reconnectToken)
                        )
                ).build());
    }

    @Override
    public OutgoingMmtpMessage createSendMessage(String subject, String sender, String body, Duration ttl) {
        long expires = Instant.now().getEpochSecond() + ttl.getSeconds();
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

    @Override
    public OutgoingMmtpMessage createSubscribeMessage(String subject) {
        return new OutgoingMmtpMessage(MmtpMessage.newBuilder()
                .setMsgType(MsgType.PROTOCOL_MESSAGE)
                .setUuid(UUID.randomUUID().toString())
                .setProtocolMessage(ProtocolMessage.newBuilder()
                        .setProtocolMsgType(ProtocolMessageType.SUBSCRIBE_MESSAGE)
                        .setSubscribeMessage(Subscribe.newBuilder()
                                .setSubject(subject)
                        )
                ).build());
    }

    @Override
    public OutgoingMmtpMessage createUnsubscribeMessage(String subject) {
        return new OutgoingMmtpMessage(MmtpMessage.newBuilder()
                .setMsgType(MsgType.PROTOCOL_MESSAGE)
                .setUuid(UUID.randomUUID().toString())
                .setProtocolMessage(ProtocolMessage.newBuilder()
                        .setProtocolMsgType(ProtocolMessageType.UNSUBSCRIBE_MESSAGE)
                        .setUnsubscribeMessage(Unsubscribe.newBuilder()
                                .setSubject(subject)
                        )
                ).build());
    }




}
