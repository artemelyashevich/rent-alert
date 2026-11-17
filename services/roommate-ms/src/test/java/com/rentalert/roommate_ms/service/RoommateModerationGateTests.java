package com.rentalert.roommate_ms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.dto.roommate.ModerationSubjectType;
import com.rentalert.roommate_ms.domain.RoommateModerationTask;
import com.rentalert.roommate_ms.dto.ModerationVerdict;
import com.rentalert.roommate_ms.repository.RoommateModerationTaskRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RoommateModerationGateTests {

    private final TextModerationService textModerationService = mock(TextModerationService.class);
    private final RoommateModerationTaskRepository taskRepository = mock(RoommateModerationTaskRepository.class);
    private final RoommateModerationGate gate = new RoommateModerationGate(textModerationService, taskRepository);

    private void pendingTask(RoommateModerationTask task) {
        when(taskRepository.findFirstBySubjectTypeAndSubjectIdAndFieldAndStatus(
            ModerationSubjectType.PROFILE, 5L, "bio", ModerationStatus.PENDING_MANUAL)).thenReturn(Optional.ofNullable(task));
    }

    @Test
    void rejectedTextFailsWithReason() {
        when(textModerationService.check("bad")).thenReturn(ModerationVerdict.rejected("нельзя"));

        assertThatThrownBy(() -> gate.moderate(ModerationSubjectType.PROFILE, 5L, "bio", "bad"))
            .isInstanceOfSatisfying(AppException.class, e -> {
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX400_ROOMMATE_TEXT_REJECTED);
                assertThat(e.getMessage()).isEqualTo("нельзя");
            });
        verify(taskRepository, never()).save(any());
    }

    @Test
    void suspiciousTextCreatesManualTask() {
        when(textModerationService.check("tg @me")).thenReturn(ModerationVerdict.pending("контакты"));
        pendingTask(null);

        assertThat(gate.moderate(ModerationSubjectType.PROFILE, 5L, "bio", "tg @me")).isEqualTo(ModerationStatus.PENDING_MANUAL);
        verify(taskRepository).save(argThat(task -> task.getSubjectId() == 5L
            && task.getField().equals("bio")
            && task.getStatus() == ModerationStatus.PENDING_MANUAL
            && task.getSubmittedText().equals("tg @me")
            && task.getDecisionReason().equals("контакты")));
    }

    @Test
    void resubmissionReplacesTextOfPendingTask() {
        RoommateModerationTask existing = RoommateModerationTask.builder().submittedText("old").build();
        when(textModerationService.check("new @me")).thenReturn(ModerationVerdict.pending("контакты"));
        pendingTask(existing);

        gate.moderate(ModerationSubjectType.PROFILE, 5L, "bio", "new @me");

        assertThat(existing.getSubmittedText()).isEqualTo("new @me");
        verify(taskRepository).save(existing);
    }

    @Test
    void approvedTextDropsPendingTask() {
        RoommateModerationTask existing = RoommateModerationTask.builder().build();
        when(textModerationService.check("fine")).thenReturn(ModerationVerdict.approved());
        pendingTask(existing);

        assertThat(gate.moderate(ModerationSubjectType.PROFILE, 5L, "bio", "fine")).isEqualTo(ModerationStatus.APPROVED_AUTO);
        verify(taskRepository).delete(existing);
    }
}
