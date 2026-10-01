package com.rentalert.identity_ms.service;

public interface BotStatusService {

    void markBotBlocked(Long userId);

    void markBotUnblocked(Long userId);
}
