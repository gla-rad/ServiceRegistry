package net.maritimeconnectivity.serviceregistry.components.mms;
import net.maritimeconnectivity.mmtp.MmtpMessage;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public interface MmtpFactory {

    MmtpMessage createConnectMessage();

    MmtpMessage createSendMessage(
            String subject,
            String sender,
            String body,
            Duration ttl
    );

    MmtpMessage createReceiveMessage();

    MmtpMessage createDisconnectMessage();
}