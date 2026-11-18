package com.rentalert.roommate_ms.service;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.dto.roommate.ModerationSubjectType;
import com.rentalert.roommate_ms.domain.RoommateModerationTask;
import com.rentalert.roommate_ms.dto.ModerationVerdict;
import com.rentalert.roommate_ms.repository.RoommateModerationTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoommateModerationGate {

    private final TextModerationService textModerationService;
    private final RoommateModerationTaskRepository taskRepository;

    public ModerationStatus moderate(ModerationSubjectType subjectType, Long subjectId, String field, String submittedText) {
        ModerationVerdict verdict = textModerationService.check(submittedText);
        if (verdict.isRejected()) {
            throw new AppException(InternalExceptionCode.EX400_ROOMMATE_TEXT_REJECTED, verdict.reason(), HttpStatus.BAD_REQUEST);
        }

        RoommateModerationTask existingTask = taskRepository
            .findFirstBySubjectTypeAndSubjectIdAndFieldAndStatus(subjectType, subjectId, field, ModerationStatus.PENDING_MANUAL)
            .orElse(null);

        if (verdict.isPending()) {
            RoommateModerationTask task = existingTask != null
                ? existingTask
                : RoommateModerationTask.builder()
                    .subjectType(subjectType)
                    .subjectId(subjectId)
                    .field(field)
                    .status(ModerationStatus.PENDING_MANUAL)
                    .build();
            task.setSubmittedText(submittedText);
            task.setDecisionReason(verdict.reason());
            taskRepository.save(task);
        } else if (existingTask != null) {
            taskRepository.delete(existingTask);
        }
        return verdict.status();
    }
}
