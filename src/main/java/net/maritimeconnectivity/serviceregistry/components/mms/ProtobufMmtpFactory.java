package net.maritimeconnectivity.serviceregistry.components.mms;

import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.mmtp.MmtpMessage;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
@Slf4j
public class ProtobufMmtpFactory implements MmtpFactory {

    @Override
    public MmtpMessage createConnectMessage() {
        // Implementation for creating a connect message
        return null; // Replace with actual implementation
    }

    @Override
    public MmtpMessage createSendMessage(String subject, String sender, String body, Duration ttl) {
        return null;
    }

    @Override
    public MmtpMessage createReceiveMessage() {
        // Implementation for creating a receive message
        return null; // Replace with actual implementation
    }

    @Override
    public MmtpMessage createDisconnectMessage() {
        // Implementation for creating a disconnect message
        return null; // Replace with actual implementation
    }




}
