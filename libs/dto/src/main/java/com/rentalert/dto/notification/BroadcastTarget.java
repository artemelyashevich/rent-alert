package com.rentalert.dto.notification;

import com.rentalert.dto.profile.BotStatus;

public record BroadcastTarget(Long userId, String telegramIdEnc, BotStatus botStatus) {

}
