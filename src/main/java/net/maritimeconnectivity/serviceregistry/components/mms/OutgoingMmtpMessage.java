package net.maritimeconnectivity.serviceregistry.components.mms;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.mmtp.MmtpMessage;

@Getter
@Slf4j
public class OutgoingMmtpMessage {

    private final MmtpMessage message;

    // Keep message state
    private boolean acknowledged;
    private int retryCount;
    private long lastSentTimestamp;

    public OutgoingMmtpMessage(MmtpMessage message) {
        this.message = message;
        this.acknowledged = false;
        this.retryCount = 0;
        this.lastSentTimestamp = System.currentTimeMillis();
    }

    public void markAcknowledged() {
        this.acknowledged = true;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void updateTimestamp() {
        this.lastSentTimestamp = System.currentTimeMillis();
    }
}
