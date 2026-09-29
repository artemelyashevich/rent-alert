package com.rentalert.processing_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(
    name = "currency_rates",
    indexes = {
        @Index(name = "idx_currency_rates_date", columnList = "rate_date"),
        @Index(name = "idx_currency_rates_currency", columnList = "currency, per_currency")
    }
)
public class CurrencyRate extends AbstractEntity {

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "per_currency", nullable = false, length = 3)
    private String perCurrency;

    @Column(name = "rate", nullable = false, precision = 18, scale = 6)
    private BigDecimal rate;
}
