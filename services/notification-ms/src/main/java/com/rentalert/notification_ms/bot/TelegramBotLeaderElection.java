package com.rentalert.notification_ms.bot;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;

@Slf4j
@Component
@ConditionalOnProperty(value = "app.telegram.bot-enabled", havingValue = "true")
@RequiredArgsConstructor
public class TelegramBotLeaderElection {

    static final String LEASE_KEY = "lock:telegram-bot-leader";
    static final Duration LEASE = Duration.ofSeconds(30);

    static final RedisScript<Long> ACQUIRE_OR_RENEW_SCRIPT = new DefaultRedisScript<>("""
        local current = redis.call('get', KEYS[1])
        if current == false or current == ARGV[1] then
          redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[2])
          return 1
        end
        return 0
        """, Long.class);

    static final RedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
        "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);

    private final TelegramBot bot;
    private final TelegramBotsApi botsApi;
    private final StringRedisTemplate redisTemplate;
    private final String instanceId = UUID.randomUUID().toString();
    private BotSession session;

    @Scheduled(fixedDelayString = "PT10S")
    public synchronized void elect() {
        boolean leader;
        try {
            leader = acquireOrRenew();
        } catch (RuntimeException e) {
            log.warn("Telegram bot lease check failed, polling is stopped on this instance: {}", e.getMessage());
            leader = false;
        }
        if (leader && session == null) {
            start();
        } else if (!leader && session != null) {
            stop();
        }
    }

    @PreDestroy
    public synchronized void shutdown() {
        if (session != null) {
            stop();
        }
        try {
            redisTemplate.execute(RELEASE_SCRIPT, List.of(LEASE_KEY), instanceId);
        } catch (RuntimeException e) {
            log.warn("Failed to release Telegram bot lease: {}", e.getMessage());
        }
    }

    private boolean acquireOrRenew() {
        Long result = redisTemplate.execute(ACQUIRE_OR_RENEW_SCRIPT, List.of(LEASE_KEY), instanceId,
            String.valueOf(LEASE.toMillis()));
        return Long.valueOf(1).equals(result);
    }

    private void start() {
        try {
            session = botsApi.registerBot(bot);
            log.info("Telegram bot polling started on instance {}", instanceId);
        } catch (TelegramApiException e) {
            log.error("Failed to start Telegram bot polling, will retry", e);
        }
    }

    private void stop() {
        session.stop();
        session = null;
        log.info("Telegram bot polling stopped on instance {}", instanceId);
    }
}
