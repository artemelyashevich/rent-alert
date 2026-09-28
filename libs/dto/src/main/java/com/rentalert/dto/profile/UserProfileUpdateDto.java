package com.rentalert.dto.profile;

import com.rentalert.dto.AbstractDto;
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
public class UserProfileUpdateDto extends AbstractDto {

    @NotNull(message = "Статус бота не может быть пустым при обновлении")
    private BotStatus botStatus;

    private LocalTime quietFrom;

    private LocalTime quietTo;
}
