package com.rentalert.identity_ms.service;

import com.rentalert.dto.profile.TelegramUser;

public interface TelegramInitDataParser {

    TelegramUser verify(String initData);
}
