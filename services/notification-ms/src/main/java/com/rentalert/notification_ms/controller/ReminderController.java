package com.rentalert.notification_ms.controller;

import com.rentalert.dto.notification.ReminderRequestDto;
import com.rentalert.dto.notification.ReminderResponseDto;
import com.rentalert.notification_ms.config.PublicUrls;
import com.rentalert.notification_ms.service.ReminderPersistenceService;
import com.rentalert.redis.service.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reminders", description = "Управление напоминаниями о просмотрах")
@RestController
@RequestMapping(PublicUrls.REMINDER_TAG)
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderPersistenceService reminderPersistenceService;
    private final UserSessionService userSessionService;

    @Operation(summary = "Получить напоминания текущего пользователя")
    @GetMapping
    public List<ReminderResponseDto> getReminders() {
        Long userId = userSessionService.getCurrentUser().getUserId();
        return reminderPersistenceService.findAllForUser(userId);
    }

    @Operation(summary = "Создать напоминание о просмотре для избранного объявления")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderResponseDto createReminder(@Valid @RequestBody ReminderRequestDto requestDto) {
        Long userId = userSessionService.getCurrentUser().getUserId();
        return reminderPersistenceService.save(requestDto, userId);
    }

    @Operation(summary = "Отменить ещё не отправленное напоминание")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelReminder(@Parameter(description = "ID напоминания") @PathVariable Long id) {
        Long userId = userSessionService.getCurrentUser().getUserId();
        reminderPersistenceService.cancel(id, userId);
    }
}
