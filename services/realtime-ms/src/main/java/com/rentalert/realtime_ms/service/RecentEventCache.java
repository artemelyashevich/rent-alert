package com.rentalert.realtime_ms.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bounded, time-limited memory of delivered event ids, used to drop Kafka redeliveries on one instance.
 * <p>
 * Entries older than the TTL and entries beyond the size limit (oldest first) are evicted. The state is local to the
 * instance, which is enough because every instance consumes each event exactly once from its own consumer group.
 */
public class RecentEventCache {

    private final Clock clock;
    private final Map<UUID, Instant> deliveredAt;
    private final Duration ttl;

    public RecentEventCache(int maxSize, Duration ttl, Clock clock) {
        this.clock = clock;
        this.ttl = ttl;
        this.deliveredAt = new LinkedHashMap<>(16, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, Instant> eldest) {
                return size() > maxSize;
            }
        };
    }

    /**
     * Whether the event was added and has not expired yet.
     */
    public synchronized boolean contains(UUID eventId) {
        evictExpired();
        return deliveredAt.containsKey(eventId);
    }

    /**
     * Remembers an event that was delivered, starting its TTL now.
     */
    public synchronized void add(UUID eventId) {
        evictExpired();
        deliveredAt.put(eventId, clock.instant());
    }

    private void evictExpired() {
        Instant cutoff = clock.instant().minus(ttl);
        Iterator<Map.Entry<UUID, Instant>> iterator = deliveredAt.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().isAfter(cutoff)) {
                break;
            }
            iterator.remove();
        }
    }
}
