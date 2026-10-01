package com.rentalert.notification_ms.repository;

import com.rentalert.notification_ms.domain.TelegramMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TelegramMessageRepository extends JpaRepository<TelegramMessage, Long> {

    @Modifying
    @Query("delete from TelegramMessage e where e.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
