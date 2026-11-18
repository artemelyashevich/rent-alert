package com.rentalert.notification_ms.bot;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;

class TelegramBotLeaderElectionTests {

    private final TelegramBot bot = mock(TelegramBot.class);
    private final TelegramBotsApi botsApi = mock(TelegramBotsApi.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final BotSession session = mock(BotSession.class);
    private TelegramBotLeaderElection election;

    @BeforeEach
    void setUp() throws TelegramApiException {
        when(botsApi.registerBot(bot)).thenReturn(session);
        election = new TelegramBotLeaderElection(bot, botsApi, redisTemplate);
    }

    private void leaseResults(Long first, Long... rest) {
        when(redisTemplate.execute(eq(TelegramBotLeaderElection.ACQUIRE_OR_RENEW_SCRIPT), anyList(), anyString(), anyString()))
            .thenReturn(first, rest);
    }

    @Test
    void leaderStartsPolling() throws TelegramApiException {
        leaseResults(1L);

        election.elect();

        verify(botsApi, times(1)).registerBot(bot);
    }

    @Test
    void leaderRenewsWithoutRestartingPolling() throws TelegramApiException {
        leaseResults(1L, 1L, 1L);

        election.elect();
        election.elect();
        election.elect();

        verify(botsApi, times(1)).registerBot(bot);
        verify(session, never()).stop();
    }

    @Test
    void nonLeaderDoesNotPoll() throws TelegramApiException {
        leaseResults(0L);

        election.elect();

        verify(botsApi, never()).registerBot(bot);
    }

    @Test
    void pollingStopsWhenLeaseIsTakenOver() {
        leaseResults(1L, 0L);

        election.elect();
        election.elect();

        verify(session, times(1)).stop();
    }

    @Test
    void pollingStopsWhenLeaseCheckFails() {
        when(redisTemplate.execute(eq(TelegramBotLeaderElection.ACQUIRE_OR_RENEW_SCRIPT), anyList(), anyString(), anyString()))
            .thenReturn(1L)
            .thenThrow(new RedisConnectionFailureException("redis is down"));

        election.elect();
        election.elect();

        verify(session, times(1)).stop();
    }

    @Test
    void failedStartIsRetriedOnNextTick() throws TelegramApiException {
        when(botsApi.registerBot(bot)).thenThrow(new TelegramApiException("boom")).thenReturn(session);
        leaseResults(1L, 1L);

        election.elect();
        election.elect();

        verify(botsApi, times(2)).registerBot(bot);
    }

    @Test
    void shutdownStopsPollingAndReleasesLease() {
        leaseResults(1L);

        election.elect();
        election.shutdown();

        verify(session, times(1)).stop();
        verify(redisTemplate).execute(eq(TelegramBotLeaderElection.RELEASE_SCRIPT), anyList(), anyString());
    }
}
