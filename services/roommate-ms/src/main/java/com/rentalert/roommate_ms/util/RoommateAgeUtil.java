package com.rentalert.roommate_ms.util;

import java.time.Year;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RoommateAgeUtil {

    public static Integer age(Short birthYear) {
        if (birthYear == null) {
            return null;
        }
        int age = Year.now().getValue() - birthYear;
        return age < 0 ? null : age;
    }

    public static String ageRange(Short birthYear) {
        Integer age = age(birthYear);
        if (age == null) {
            return null;
        }
        int lo = age / 5 * 5;
        return lo + "–" + (lo + 4);
    }
}
