package net.maritimeconnectivity.serviceregistry.components.mms;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public interface OutgoingMmtpFactory {

    OutgoingMmtpMessage createConnectMessage();

    OutgoingMmtpMessage createSendMessage(
            String subject,
            String sender,
            String body,
            Duration ttl
    );

    OutgoingMmtpMessage createReceiveMessage();

    OutgoingMmtpMessage createDisconnectMessage();
}