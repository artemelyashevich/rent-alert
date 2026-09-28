package com.rentalert.notification_ms.repository;

import com.rentalert.notification_ms.domain.TelegramMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramMessageRepository extends JpaRepository<TelegramMessage, Long> {
}
