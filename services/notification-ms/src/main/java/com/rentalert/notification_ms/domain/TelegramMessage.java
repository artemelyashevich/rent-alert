package com.rentalert.notification_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import com.rentalert.notification_ms.bot.TelegramCommand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Builder
@Getter
@Setter
@Table(name = "telegram_messages")
@NoArgsConstructor
@AllArgsConstructor
public class TelegramMessage extends AbstractEntity {

    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    private TelegramCommand command;
}
