package com.rentalert.roommate_ms.service.impl;

import com.rentalert.roommate_ms.domain.LivingSchedule;
import com.rentalert.roommate_ms.domain.RoommateListing;
import com.rentalert.roommate_ms.domain.RoommateProfile;
import com.rentalert.roommate_ms.domain.SmokingPolicy;
import com.rentalert.roommate_ms.service.RoommateScoringService;
import com.rentalert.roommate_ms.util.RoommateAgeUtil;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RoommateScoringServiceImpl implements RoommateScoringService {

    private static final double NEUTRAL = 0.5;

    private static final int W_TAGS = 45;
    private static final int W_SCHEDULE = 15;
    private static final int W_CLEANLINESS = 15;
    private static final int W_SMOKING = 15;
    private static final int W_BUDGET = 10;

    private static final int WL_TAGS = 40;
    private static final int WL_BUDGET = 40;
    private static final int WL_AGE = 20;

    @Override
    public int score(RoommateProfile viewer, RoommateProfile candidate) {
        double score = tagJaccard(viewer.getLifestyleTags(), candidate.getLifestyleTags()) * W_TAGS
            + scheduleAffinity(viewer.getSchedule(), candidate.getSchedule()) * W_SCHEDULE
            + cleanlinessAffinity(viewer.getCleanliness(), candidate.getCleanliness()) * W_CLEANLINESS
            + smokingAffinity(viewer.getSmoking(), candidate.getSmoking()) * W_SMOKING
            + budgetOverlap(viewer.getBudgetMin(), viewer.getBudgetMax(), candidate.getBudgetMin(), candidate.getBudgetMax()) * W_BUDGET;
        return clamp(score);
    }

    @Override
    public int scoreListing(RoommateProfile viewer, RoommateListing listing) {
        double score = tagJaccard(viewer.getLifestyleTags(), listing.getHouseRules()) * WL_TAGS
            + rentFit(viewer.getBudgetMin(), viewer.getBudgetMax(), listing.getRentShare()) * WL_BUDGET
            + ageFit(RoommateAgeUtil.age(viewer.getBirthYear()), listing.getAgeMin(), listing.getAgeMax()) * WL_AGE;
        return clamp(score);
    }

    @Override
    public List<String> sharedTags(RoommateProfile viewer, RoommateProfile candidate) {
        Set<String> shared = toSet(viewer.getLifestyleTags());
        shared.retainAll(toSet(candidate.getLifestyleTags()));
        return List.copyOf(shared);
    }

    private static double tagJaccard(String[] a, String[] b) {
        Set<String> union = toSet(a);
        Set<String> intersection = toSet(a);
        Set<String> other = toSet(b);
        union.addAll(other);
        intersection.retainAll(other);
        return union.isEmpty() ? NEUTRAL : (double) intersection.size() / union.size();
    }

    private static double scheduleAffinity(LivingSchedule a, LivingSchedule b) {
        if (a == null || b == null) {
            return NEUTRAL;
        }
        if (a == b) {
            return 1.0;
        }
        return a == LivingSchedule.FLEXIBLE || b == LivingSchedule.FLEXIBLE ? 0.75 : 0.0;
    }

    private static double cleanlinessAffinity(Short a, Short b) {
        if (a == null || b == null) {
            return NEUTRAL;
        }
        return 1.0 - Math.abs(a - b) / 4.0;
    }

    private static double smokingAffinity(SmokingPolicy a, SmokingPolicy b) {
        if (a == null || b == null) {
            return NEUTRAL;
        }
        if (a == b) {
            return 1.0;
        }
        return a != SmokingPolicy.YES && b != SmokingPolicy.YES ? 0.5 : 0.0;
    }

    private static double budgetOverlap(Integer vMin, Integer vMax, Integer cMin, Integer cMax) {
        if (vMin == null || vMax == null || cMin == null || cMax == null) {
            return NEUTRAL;
        }
        double overlap = Math.max(0, Math.min(vMax, cMax) - Math.max(vMin, cMin));
        double union = Math.max(vMax, cMax) - Math.min(vMin, cMin);
        return union <= 0 ? 1.0 : overlap / union;
    }

    private static double rentFit(Integer vMin, Integer vMax, BigDecimal rent) {
        if (rent == null || vMax == null) {
            return NEUTRAL;
        }
        double r = rent.doubleValue();
        double lo = vMin == null ? 0 : vMin;
        if (r >= lo && r <= vMax) {
            return 1.0;
        }
        double miss = r > vMax ? r - vMax : lo - r;
        double span = Math.max(1, vMax - lo);
        return Math.max(0.0, 1.0 - miss / span);
    }

    private static double ageFit(Integer age, Integer ageMin, Integer ageMax) {
        if (age == null || ageMin == null && ageMax == null) {
            return NEUTRAL;
        }
        boolean okLow = ageMin == null || age >= ageMin;
        boolean okHigh = ageMax == null || age <= ageMax;
        if (okLow && okHigh) {
            return 1.0;
        }
        return okLow || okHigh ? 0.4 : 0.0;
    }

    private static Set<String> toSet(String[] values) {
        if (values == null) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(values)
            .filter(value -> value != null && !value.isBlank())
            .map(value -> value.trim().toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static int clamp(double raw) {
        return (int) Math.round(Math.max(0, Math.min(100, raw)));
    }
}
