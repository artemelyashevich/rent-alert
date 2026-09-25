package com.rentalert.dto.currency;

public enum Currency {
    USD, BYN, EUR;

    public static Currency getName(String name) {
        for (Currency value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return BYN;
    }
}
