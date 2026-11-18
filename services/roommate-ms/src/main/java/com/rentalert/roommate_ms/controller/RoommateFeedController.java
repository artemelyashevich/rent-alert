package com.rentalert.roommate_ms.controller;

import com.rentalert.redis.service.UserSessionService;
import com.rentalert.roommate_ms.config.PublicUrls;
import com.rentalert.roommate_ms.dto.RoommateCandidateView;
import com.rentalert.roommate_ms.dto.RoommateFeedPage;
import com.rentalert.roommate_ms.dto.RoommateListingCardView;
import com.rentalert.roommate_ms.dto.RoommateMatchView;
import com.rentalert.roommate_ms.service.RoommateFeedService;
import com.rentalert.roommate_ms.service.RoommateHiddenService;
import com.rentalert.roommate_ms.service.RoommateMatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Roommate feed", description = "Лента кандидатов и объявлений о подселении")
@RestController
@RequestMapping(PublicUrls.FEED_TAG)
@RequiredArgsConstructor
public class RoommateFeedController {

    private final RoommateFeedService feedService;
    private final RoommateHiddenService hiddenService;
    private final RoommateMatchService matchService;
    private final UserSessionService userSessionService;

    @Operation(summary = "Лента кандидатов-людей", description = "Keyset-пагинация: cursor из предыдущего ответа.")
    @GetMapping("/roommates")
    public RoommateFeedPage<RoommateCandidateView> candidates(
        @Parameter(description = "Курсор из предыдущего ответа") @RequestParam(required = false) String cursor,
        @Parameter(description = "Размер страницы, по умолчанию 20, максимум 50") @RequestParam(required = false) Integer size
    ) {
        return feedService.candidates(currentUserId(), cursor, size);
    }

    @Operation(summary = "Лента объявлений о подселении")
    @GetMapping("/listings")
    public RoommateFeedPage<RoommateListingCardView> listings(
        @Parameter(description = "Курсор из предыдущего ответа") @RequestParam(required = false) String cursor,
        @Parameter(description = "Размер страницы, по умолчанию 20, максимум 50") @RequestParam(required = false) Integer size
    ) {
        return feedService.listings(currentUserId(), cursor, size);
    }

    @Operation(summary = "Лайкнуть кандидата", description = "Идемпотентно; при взаимном интересе матч сразу переходит в MATCHED.")
    @PostMapping("/roommates/{candidateUserId}/like")
    @ResponseStatus(HttpStatus.CREATED)
    public RoommateMatchView likeCandidate(@PathVariable Long candidateUserId) {
        return matchService.like(currentUserId(), candidateUserId);
    }

    @Operation(summary = "Скрыть кандидата в ленте")
    @PostMapping("/roommates/{candidateUserId}/skip")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void skipCandidate(@PathVariable Long candidateUserId) {
        hiddenService.skipCandidate(currentUserId(), candidateUserId);
    }

    @Operation(summary = "Лайкнуть объявление о подселении", description = "Создаёт/двигает матч с автором объявления (сценарий ROOM).")
    @PostMapping("/listings/{listingId}/like")
    @ResponseStatus(HttpStatus.CREATED)
    public RoommateMatchView likeListing(@PathVariable Long listingId) {
        return matchService.likeListing(currentUserId(), listingId);
    }

    @Operation(summary = "Скрыть объявление в ленте")
    @PostMapping("/listings/{listingId}/skip")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void skipListing(@PathVariable Long listingId) {
        hiddenService.skipListing(currentUserId(), listingId);
    }

    private Long currentUserId() {
        return userSessionService.getCurrentUser().getUserId();
    }
}
