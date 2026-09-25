package com.rentalert.dto.currency;

import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CurrencyPair {
    BYN_USD("BYN", "USD"),
    BYN_EUR("BYN", "EUR"),
    EUR_BYN("EUR", "BYN"),
    USD_BYN("USD", "BYN"),
    USD_EUR("USD", "EUR"),
    EUR_USD("EUR", "USD");

    private final String from;
    private final String to;

    public static List<CurrencyPair> getPairsByMain(Currency currency) {
        return Arrays.stream(values())
                .filter(pair -> pair.from.equalsIgnoreCase(currency.name()))
                .toList();
    }

    public static CurrencyPair getPair(Currency from, Currency to) {
        return Arrays.stream(values())
                .filter(pair ->
                        pair.from.equalsIgnoreCase(from.name())
                                && pair.to.equalsIgnoreCase(to.name())
                )
                .findFirst()
                .orElse(null);
    }
}
