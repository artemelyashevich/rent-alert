package com.rentalert.roommate_ms.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rentalert.roommate_ms.domain.LivingSchedule;
import com.rentalert.roommate_ms.domain.RoommateListing;
import com.rentalert.roommate_ms.domain.RoommateProfile;
import com.rentalert.roommate_ms.domain.SmokingPolicy;
import com.rentalert.roommate_ms.service.impl.RoommateScoringServiceImpl;
import java.math.BigDecimal;
import java.time.Year;
import org.junit.jupiter.api.Test;

class RoommateScoringServiceImplTests {

    private final RoommateScoringService service = new RoommateScoringServiceImpl();

    private static RoommateProfile profile(String... tags) {
        return RoommateProfile.builder()
            .lifestyleTags(tags)
            .schedule(LivingSchedule.EARLY_BIRD)
            .cleanliness((short) 4)
            .smoking(SmokingPolicy.NO)
            .budgetMin(300)
            .budgetMax(500)
            .build();
    }

    @Test
    void identicalProfilesScoreFull() {
        assertThat(service.score(profile("sport", "books"), profile("Books ", "SPORT"))).isEqualTo(100);
    }

    @Test
    void unansweredProfilesAreNeutral() {
        assertThat(service.score(new RoommateProfile(), new RoommateProfile())).isEqualTo(50);
    }

    @Test
    void conflictingHabitsLowerTheScore() {
        RoommateProfile smoker = profile("party");
        smoker.setSmoking(SmokingPolicy.YES);
        smoker.setSchedule(LivingSchedule.NIGHT_OWL);
        smoker.setCleanliness((short) 1);
        smoker.setBudgetMin(600);
        smoker.setBudgetMax(800);

        assertThat(service.score(profile("books"), smoker)).isEqualTo(4);
    }

    @Test
    void sharedTagsAreNormalized() {
        assertThat(service.sharedTags(profile("Sport", "books"), profile("sport", "music"))).containsExactly("sport");
    }

    @Test
    void listingWithinBudgetAndAgeScoresFull() {
        RoommateProfile viewer = profile("quiet");
        viewer.setBirthYear((short) (Year.now().getValue() - 25));
        RoommateListing listing = RoommateListing.builder()
            .houseRules(new String[]{"quiet"})
            .rentShare(new BigDecimal("400"))
            .ageMin(20)
            .ageMax(30)
            .build();

        assertThat(service.scoreListing(viewer, listing)).isEqualTo(100);
    }

    @Test
    void rentAboveBudgetFadesOut() {
        RoommateListing pricey = RoommateListing.builder().houseRules(new String[]{"quiet"}).rentShare(new BigDecimal("600")).build();

        assertThat(service.scoreListing(profile("quiet"), pricey)).isEqualTo(70);
    }
}
