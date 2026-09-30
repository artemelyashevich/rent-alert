package com.rentalert.dto.listing;

import java.util.Arrays;

public enum ScheduleProvider {
    KUFAR, REALT, ONLINER;

    public static ScheduleProvider getScheduleProvider(String provider) {
        return Arrays.stream(values())
            .filter(p -> p.name().equalsIgnoreCase(provider))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Invalid scheduler provider value"));
    }
}
