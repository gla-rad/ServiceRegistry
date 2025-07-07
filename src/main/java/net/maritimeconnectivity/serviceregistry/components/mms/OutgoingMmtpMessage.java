package net.maritimeconnectivity.serviceregistry.components.mms;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.mmtp.MmtpMessage;

import java.util.UUID;

@Getter
@Slf4j
public class OutgoingMmtpMessage {

    private final MmtpMessage message;
    @Setter
    private String gsrUuid;

    // Keep message state
    private int sendAttempts;
    private long lastSentTimestamp;

    public OutgoingMmtpMessage(MmtpMessage message) {
        this.message = message;
        this.sendAttempts = 0;
        this.lastSentTimestamp = System.currentTimeMillis();
        this.gsrUuid = null;
    }
    public void incrementSendAttempts() {
        this.sendAttempts++;
    }

    public void updateTimestamp() {
        this.lastSentTimestamp = System.currentTimeMillis();
    }

}
