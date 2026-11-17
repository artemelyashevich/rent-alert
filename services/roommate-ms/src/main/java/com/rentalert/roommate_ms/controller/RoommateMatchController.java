package com.rentalert.roommate_ms.controller;

import com.rentalert.redis.service.UserSessionService;
import com.rentalert.roommate_ms.config.PublicUrls;
import com.rentalert.roommate_ms.domain.RoommateMatchStatus;
import com.rentalert.roommate_ms.dto.RoommateContactResponse;
import com.rentalert.roommate_ms.dto.RoommateMatchAction;
import com.rentalert.roommate_ms.dto.RoommateMatchView;
import com.rentalert.roommate_ms.dto.RoommateTransitionRequest;
import com.rentalert.roommate_ms.service.RoommateMatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Roommate matches", description = "Матчи пары: статусы и действия над ними, контакты собеседника")
@RestController
@RequestMapping(PublicUrls.MATCH_TAG)
@RequiredArgsConstructor
public class RoommateMatchController {

    private final RoommateMatchService matchService;
    private final UserSessionService userSessionService;

    /**
     * The current user's matches, newest first, optionally filtered by status.
     */
    @Operation(summary = "Мои матчи", description = "Опционально фильтр по статусам.")
    @GetMapping
    public Page<RoommateMatchView> myMatches(
        @Parameter(description = "Фильтр по статусам матча") @RequestParam(required = false) List<RoommateMatchStatus> status,
        @Parameter(description = "Параметры пагинации и сортировки") @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return matchService.myMatches(currentUserId(), status, pageable);
    }

    /**
     * One match of the current user; 403 if the user is not a participant.
     */
    @Operation(summary = "Карточка матча")
    @GetMapping("/{matchId}")
    public RoommateMatchView getMatch(@PathVariable Long matchId) {
        return matchService.getMatch(matchId, currentUserId());
    }

    /**
     * Applies the action to the match. {@code expectedVersion} must equal the {@code statusVersion} from the last
     * response, otherwise 409; the body may be omitted for actions without parameters.
     */
    @Operation(
        summary = "Выполнить действие над матчем",
        description = "ACCEPT · DECLINE · START_CHAT · SCHEDULE_VIEWING · CANCEL_VIEWING · CONFIRM_MOVE_IN. "
            + "expectedVersion — statusVersion из последнего ответа; при рассинхроне — 409."
    )
    @PostMapping("/{matchId}/actions/{action}")
    public RoommateMatchView transition(
        @PathVariable Long matchId,
        @PathVariable RoommateMatchAction action,
        @RequestBody(required = false) RoommateTransitionRequest request
    ) {
        RoommateTransitionRequest body = request == null ? new RoommateTransitionRequest(null, null, null, null) : request;
        return matchService.transition(matchId, currentUserId(), action, body.toCommand(), body.expectedVersion());
    }

    /**
     * The counterparty's contact; 403 until the match has unlocked contacts (MATCHED).
     */
    @Operation(summary = "Контакты второй стороны", description = "Доступно только после MATCHED.")
    @GetMapping("/{matchId}/contacts")
    public RoommateContactResponse getContacts(@PathVariable Long matchId) {
        return matchService.getContacts(matchId, currentUserId());
    }

    private Long currentUserId() {
        return userSessionService.getCurrentUser().getUserId();
    }
}
