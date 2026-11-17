package com.rentalert.notification_ms.messaging;

import com.rentalert.kafka.inbox.InboxGuard;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

class InMemoryInboxGuard extends InboxGuard {

    private final Set<String> processed = new HashSet<>();

    InMemoryInboxGuard() {
        super(null, null, Duration.ofDays(7));
    }

    @Override
    public boolean processOnce(String consumer, UUID idempotencyKey, Runnable handler) {
        String marker = consumer + ":" + idempotencyKey;
        if (processed.contains(marker)) {
            return false;
        }
        handler.run();
        processed.add(marker);
        return true;
    }
}
