package com.rentalert.listing_ms.repository;

import com.rentalert.listing_ms.domain.CurrencyRate;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, Long> {

    @Query("""
        SELECT c FROM CurrencyRate c
        WHERE c.currency = :currency
          AND c.perCurrency = :perCurrency
        ORDER BY c.rateDate DESC
        LIMIT 1
        """)
    CurrencyRate findLatestByCurrencyAndPerCurrency(String currency, String perCurrency);

    boolean existsByRateDateAndCurrencyAndPerCurrency(LocalDate date, String currency, String perCurrency);
}
