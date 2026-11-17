package com.rentalert.roommate_ms.service;

import static com.rentalert.roommate_ms.domain.RoommateMatchStatus.CHATTING;
import static com.rentalert.roommate_ms.domain.RoommateMatchStatus.DECLINED;
import static com.rentalert.roommate_ms.domain.RoommateMatchStatus.MATCHED;
import static com.rentalert.roommate_ms.domain.RoommateMatchStatus.MOVED_IN;
import static com.rentalert.roommate_ms.domain.RoommateMatchStatus.VIEWING_TOGETHER;
import static com.rentalert.roommate_ms.domain.RoommateMatchStatus.WAITING_RESPONSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.roommate_ms.domain.RoommateMatchStatus;
import com.rentalert.roommate_ms.dto.RoommateMatchAction;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RoommateMatchStateMachineTests {

    private static final Set<RoommateMatchStatus> DECLINABLE =
        EnumSet.of(RoommateMatchStatus.INTERESTED, WAITING_RESPONSE, MATCHED, CHATTING, VIEWING_TOGETHER);

    private final RoommateMatchStateMachine machine = new RoommateMatchStateMachine();

    @Test
    void mainChainFollowsTheTransitionTable() {
        assertThat(machine.target(RoommateMatchAction.ACCEPT, WAITING_RESPONSE)).isEqualTo(MATCHED);
        assertThat(machine.target(RoommateMatchAction.START_CHAT, MATCHED)).isEqualTo(CHATTING);
        assertThat(machine.target(RoommateMatchAction.SCHEDULE_VIEWING, CHATTING)).isEqualTo(VIEWING_TOGETHER);
        assertThat(machine.target(RoommateMatchAction.CANCEL_VIEWING, VIEWING_TOGETHER)).isEqualTo(CHATTING);
        assertThat(machine.target(RoommateMatchAction.CONFIRM_MOVE_IN, VIEWING_TOGETHER)).isEqualTo(MOVED_IN);
    }

    @ParameterizedTest
    @EnumSource(value = RoommateMatchStatus.class, names = {"INTERESTED", "WAITING_RESPONSE", "MATCHED", "CHATTING", "VIEWING_TOGETHER"})
    void declineIsAllowedFromEveryActiveStatus(RoommateMatchStatus from) {
        assertThat(machine.target(RoommateMatchAction.DECLINE, from)).isEqualTo(DECLINED);
    }

    @ParameterizedTest
    @EnumSource(value = RoommateMatchStatus.class, names = {"DECLINED", "EXPIRED", "MOVED_IN", "ARCHIVED"})
    void declineIsRejectedFromTerminalStatus(RoommateMatchStatus from) {
        assertNotAllowed(RoommateMatchAction.DECLINE, from);
    }

    @Test
    void declineRejectsEveryStatusOutsideDeclinable() {
        for (RoommateMatchStatus from : EnumSet.complementOf(EnumSet.copyOf(DECLINABLE))) {
            assertNotAllowed(RoommateMatchAction.DECLINE, from);
        }
    }

    @Test
    void acceptIsAllowedOnlyFromWaitingResponse() {
        assertOnlyAllowed(RoommateMatchAction.ACCEPT, WAITING_RESPONSE, MATCHED);
    }

    @Test
    void startChatIsAllowedOnlyFromMatched() {
        assertOnlyAllowed(RoommateMatchAction.START_CHAT, MATCHED, CHATTING);
    }

    @Test
    void scheduleViewingIsAllowedOnlyFromChatting() {
        assertOnlyAllowed(RoommateMatchAction.SCHEDULE_VIEWING, CHATTING, VIEWING_TOGETHER);
    }

    @Test
    void cancelViewingIsAllowedOnlyFromViewingTogether() {
        assertOnlyAllowed(RoommateMatchAction.CANCEL_VIEWING, VIEWING_TOGETHER, CHATTING);
    }

    @Test
    void confirmMoveInIsAllowedOnlyFromViewingTogether() {
        assertOnlyAllowed(RoommateMatchAction.CONFIRM_MOVE_IN, VIEWING_TOGETHER, MOVED_IN);
    }

    @ParameterizedTest
    @EnumSource(value = RoommateMatchStatus.class, names = {"DECLINED", "EXPIRED", "MOVED_IN", "ARCHIVED"})
    void terminalStatusesAreTerminal(RoommateMatchStatus status) {
        assertThat(machine.isTerminal(status)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = RoommateMatchStatus.class, names = {"INTERESTED", "WAITING_RESPONSE", "MATCHED", "CHATTING", "VIEWING_TOGETHER"})
    void activeStatusesAreNotTerminal(RoommateMatchStatus status) {
        assertThat(machine.isTerminal(status)).isFalse();
    }

    private void assertOnlyAllowed(RoommateMatchAction action, RoommateMatchStatus allowedFrom, RoommateMatchStatus expectedTarget) {
        assertThat(machine.target(action, allowedFrom)).isEqualTo(expectedTarget);
        for (RoommateMatchStatus from : EnumSet.complementOf(EnumSet.of(allowedFrom))) {
            assertNotAllowed(action, from);
        }
    }

    private void assertNotAllowed(RoommateMatchAction action, RoommateMatchStatus from) {
        assertThatThrownBy(() -> machine.target(action, from))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX409_ROOMMATE_TRANSITION_NOT_ALLOWED));
    }
}
