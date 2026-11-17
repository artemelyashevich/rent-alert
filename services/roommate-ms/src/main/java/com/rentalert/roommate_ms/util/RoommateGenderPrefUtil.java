package com.rentalert.roommate_ms.util;

import com.rentalert.roommate_ms.domain.Gender;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RoommateGenderPrefUtil {

    public static List<Gender> resolve(String[] genderPref) {
        if (genderPref == null || genderPref.length == 0) {
            return List.of(Gender.values());
        }
        List<Gender> resolved = Arrays.stream(genderPref)
            .map(RoommateGenderPrefUtil::parseOrNull)
            .filter(Objects::nonNull)
            .toList();
        return resolved.isEmpty() ? List.of(Gender.values()) : resolved;
    }

    private static Gender parseOrNull(String value) {
        try {
            return Gender.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
