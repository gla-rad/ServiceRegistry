package net.maritimeconnectivity.serviceregistry.models.dto.gmsp;

import java.util.concurrent.atomic.AtomicInteger;

public class GlobalSearchRequestDto {
    private final AtomicInteger count;

    //Add list of results

    public GlobalSearchRequestDto(int outgoingMmtpMessageCount) {
        this.count = new AtomicInteger(outgoingMmtpMessageCount);
    }

    public void decrementCount() {
        this.count.decrementAndGet();
    }

    public boolean isSent() {
        return this.count.get() <= 0;
    }


}
