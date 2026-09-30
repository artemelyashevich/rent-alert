package com.rentalert.dto.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.rentalert.dto.AbstractDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@AllArgsConstructor
public class BroadcastRequest extends AbstractDto {

    @NotBlank(message = "Сообщение обязательно")
    @Size(max = 4096, message = "Сообщение слишком длинное")
    private String message;

    @Schema(description = "Список id получателей. Если не указан или пуст — рассылка отправляется всем пользователям.")
    @Size(max = 10_000, message = "Слишком много получателей в одной рассылке")
    private List<@Positive(message = "id получателя должен быть положительным") Long> userIds;

    @Schema(description = "Закрепить сообщение в чате бота у каждого получателя.", defaultValue = "false")
    private boolean pin;
}
