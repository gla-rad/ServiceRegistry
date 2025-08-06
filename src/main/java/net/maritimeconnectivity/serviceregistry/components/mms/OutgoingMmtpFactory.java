package net.maritimeconnectivity.serviceregistry.components.mms;
import org.springframework.stereotype.Component;

import java.time.Duration;

public interface OutgoingMmtpFactory {

    OutgoingMmtpMessage createConnectMessage(String ownMrn);

    OutgoingMmtpMessage createSendMessage(
            String subject,
            String sender,
            String body,
            Duration ttl
    );

    OutgoingMmtpMessage createReceiveMessage();

    OutgoingMmtpMessage createDisconnectMessage();

    OutgoingMmtpMessage createSubscribeMessage(String subject);
}