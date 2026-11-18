package com.rentalert.realtime_ms.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

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

    public synchronized boolean contains(UUID eventId) {
        evictExpired();
        return deliveredAt.containsKey(eventId);
    }

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
