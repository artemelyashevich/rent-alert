package com.rentalert.roommate_ms.service;

import static com.rentalert.roommate_ms.domain.RoommateMatchStatus.CHATTING;
import static com.rentalert.roommate_ms.domain.RoommateMatchStatus.DECLINED;
import static com.rentalert.roommate_ms.domain.RoommateMatchStatus.MATCHED;
import static com.rentalert.roommate_ms.domain.RoommateMatchStatus.MOVED_IN;
import static com.rentalert.roommate_ms.domain.RoommateMatchStatus.VIEWING_TOGETHER;
import static com.rentalert.roommate_ms.domain.RoommateMatchStatus.WAITING_RESPONSE;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.roommate_ms.domain.RoommateMatchStatus;
import com.rentalert.roommate_ms.dto.RoommateMatchAction;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RoommateMatchStateMachine {

    private static final Set<RoommateMatchStatus> TERMINAL =
        EnumSet.of(DECLINED, RoommateMatchStatus.EXPIRED, MOVED_IN, RoommateMatchStatus.ARCHIVED);

    private static final Set<RoommateMatchStatus> DECLINABLE =
        EnumSet.of(RoommateMatchStatus.INTERESTED, WAITING_RESPONSE, MATCHED, CHATTING, VIEWING_TOGETHER);

    private static final Map<RoommateMatchAction, Map<RoommateMatchStatus, RoommateMatchStatus>> EDGES =
        new EnumMap<>(RoommateMatchAction.class);

    static {
        EDGES.put(RoommateMatchAction.ACCEPT, Map.of(WAITING_RESPONSE, MATCHED));
        EDGES.put(RoommateMatchAction.START_CHAT, Map.of(MATCHED, CHATTING));
        EDGES.put(RoommateMatchAction.SCHEDULE_VIEWING, Map.of(CHATTING, VIEWING_TOGETHER));
        EDGES.put(RoommateMatchAction.CANCEL_VIEWING, Map.of(VIEWING_TOGETHER, CHATTING));
        EDGES.put(RoommateMatchAction.CONFIRM_MOVE_IN, Map.of(VIEWING_TOGETHER, MOVED_IN));
    }

    public boolean isTerminal(RoommateMatchStatus status) {
        return TERMINAL.contains(status);
    }

    public RoommateMatchStatus target(RoommateMatchAction action, RoommateMatchStatus from) {
        if (action == RoommateMatchAction.DECLINE) {
            if (!DECLINABLE.contains(from)) {
                throw notAllowed(action, from);
            }
            return DECLINED;
        }
        RoommateMatchStatus to = EDGES.getOrDefault(action, Map.of()).get(from);
        if (to == null) {
            throw notAllowed(action, from);
        }
        return to;
    }

    private AppException notAllowed(RoommateMatchAction action, RoommateMatchStatus from) {
        return new AppException(
            InternalExceptionCode.EX409_ROOMMATE_TRANSITION_NOT_ALLOWED,
            "action=" + action + " from=" + from,
            HttpStatus.CONFLICT);
    }
}
