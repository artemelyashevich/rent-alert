package com.rentalert.identity_ms.dto.profile;

import com.rentalert.dto.AbstractDto;
import com.rentalert.identity_ms.domain.BotStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileCreateDto extends AbstractDto {

    private Long id;

    @NotBlank(message = "Telegram id is required")
    private String telegramId;

    @NotBlank(message = "Username is required")
    private String username;

    @NotNull(message = "Bot status mustn't be null")
    @Builder.Default
    private BotStatus botStatus = BotStatus.ACTIVE;

    private String photoUrl;

    private LocalTime quietFrom;

    private LocalTime quietTo;
}