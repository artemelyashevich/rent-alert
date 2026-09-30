package com.rentalert.listing_ms.repository;

import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.listing.ListingCategory;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import com.rentalert.listing_ms.domain.HiddenListing;
import com.rentalert.listing_ms.domain.Listing;
import com.rentalert.listing_ms.service.CurrencyRateService;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ListingSpecification {

    private ListingSpecification() {
    }

    public static Specification<Listing> hasSources(Collection<ScheduleProvider> sources) {
        return (root, query, cb) -> {
            if (sources == null || sources.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("source").in(sources);
        };
    }

    public static Specification<Listing> matchesFilter(UserFilterResponse filter) {
        return Specification.where(hasPriceInRange(filter.getPriceFrom(), filter.getPriceTo(), filter.getCurrency()))
            .and(hasRoomsInRange(filter.getRoomsFrom(), filter.getRoomsTo()))
            .and(hasAreaInRange(filter.getAreaFrom(), filter.getAreaTo()))
            .and(hasCity(filter.getCity()));
    }

    public static Specification<Listing> matchesFilterWithCurrency(UserFilterResponse filter, CurrencyRateService currencyRateService) {
        return Specification.where(hasPriceInRangeWithConversion(filter.getPriceFrom(), filter.getPriceTo(), filter.getCurrency(), currencyRateService))
            .and(hasRoomsInRange(filter.getRoomsFrom(), filter.getRoomsTo()))
            .and(hasAreaInRange(filter.getAreaFrom(), filter.getAreaTo()))
            .and(hasCity(filter.getCity()));
    }

    public static Specification<Listing> hasCategory(ListingCategory category) {
        return (root, query, cb) -> {
            if (category == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("category"), category);
        };
    }

    public static Specification<Listing> hasCity(String city) {
        return (root, query, cb) -> {
            if (city == null || city.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase());
        };
    }

    public static Specification<Listing> hasPriceInRange(Integer priceFrom, Integer priceTo, Currency currency) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (currency != null) {
                predicates.add(cb.equal(root.get("currency"), currency));
            }

            if ((priceFrom == null || priceFrom <= 0) && (priceTo == null || priceTo <= 0)) {
                return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
            }

            if (priceFrom != null && priceFrom > 0 && priceTo != null && priceTo > 0) {
                predicates.add(cb.between(root.get("price"), priceFrom, priceTo));
            } else if (priceFrom != null && priceFrom > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), priceFrom));
            } else {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), priceTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Listing> hasPriceInRangeWithConversion(
        Integer priceFrom,
        Integer priceTo,
        Currency filterCurrency,
        CurrencyRateService currencyRateService
    ) {
        return (root, query, cb) -> {
            if ((priceFrom == null || priceFrom <= 0) && (priceTo == null || priceTo <= 0)) {
                return cb.conjunction();
            }

            final Currency effectiveFilterCurrency = (filterCurrency != null) ? filterCurrency : Currency.BYN;

            List<Predicate> currencyPredicates = new ArrayList<>();

            for (Currency listingCurrency : Currency.values()) {
                List<Predicate> currencyConditions = new ArrayList<>();
                currencyConditions.add(cb.equal(root.get("currency"), listingCurrency));

                final Integer finalPriceFrom = priceFrom;
                final Integer finalPriceTo = priceTo;

                Integer convertedFrom = finalPriceFrom;
                Integer convertedTo = finalPriceTo;

                if (!effectiveFilterCurrency.equals(listingCurrency)) {
                    if (finalPriceFrom != null && finalPriceFrom > 0) {
                        BigDecimal converted = currencyRateService.convert(
                            BigDecimal.valueOf(finalPriceFrom),
                            effectiveFilterCurrency,
                            listingCurrency
                        ).orElse(null);
                        if (converted != null) {
                            convertedFrom = converted.intValue();
                        }
                    }

                    if (finalPriceTo != null && finalPriceTo > 0) {
                        BigDecimal converted = currencyRateService.convert(
                            BigDecimal.valueOf(finalPriceTo),
                            effectiveFilterCurrency,
                            listingCurrency
                        ).orElse(null);
                        if (converted != null) {
                            convertedTo = converted.intValue();
                        }
                    }
                }

                if (convertedFrom != null && convertedFrom > 0 && convertedTo != null && convertedTo > 0) {
                    currencyConditions.add(cb.between(root.get("price"), convertedFrom, convertedTo));
                } else if (convertedFrom != null && convertedFrom > 0) {
                    currencyConditions.add(cb.greaterThanOrEqualTo(root.get("price"), convertedFrom));
                } else if (convertedTo != null && convertedTo > 0) {
                    currencyConditions.add(cb.lessThanOrEqualTo(root.get("price"), convertedTo));
                }

                currencyPredicates.add(cb.and(currencyConditions.toArray(new Predicate[0])));
            }

            return cb.or(currencyPredicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Listing> hasRoomsInRange(Integer roomsFrom, Integer roomsTo) {
        return (root, query, cb) -> {
            if ((roomsFrom == null || roomsFrom <= 0) && (roomsTo == null || roomsTo <= 0)) {
                return cb.conjunction();
            }

            if (roomsFrom != null && roomsFrom > 0 && roomsTo != null && roomsTo > 0) {
                return cb.between(root.get("rooms"), roomsFrom, roomsTo);
            } else if (roomsFrom != null && roomsFrom > 0) {
                return cb.greaterThanOrEqualTo(root.get("rooms"), roomsFrom);
            } else {
                return cb.lessThanOrEqualTo(root.get("rooms"), roomsTo);
            }
        };
    }

    /**
     * Ищет по ключевым атрибутам объявления: description, street, price, currency, title. Текстовые поля ищутся по подстроке (регистронезависимо), price и currency — по точному совпадению, если
     * строка фильтра приводится к соответствующему типу.
     */
    public static Specification<Listing> matchesKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }

            String trimmed = keyword.trim();
            String pattern = "%" + trimmed.toLowerCase() + "%";

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(root.get("description")), pattern));
            predicates.add(cb.like(cb.lower(root.get("street")), pattern));
            predicates.add(cb.like(cb.lower(root.get("title")), pattern));

            try {
                BigDecimal priceValue = new BigDecimal(trimmed);
                predicates.add(cb.equal(root.get("price"), priceValue));
            } catch (NumberFormatException ignored) {
                // filter не является числом — пропускаем поиск по price
            }

            for (Currency currency : Currency.values()) {
                if (currency.name().equalsIgnoreCase(trimmed)) {
                    predicates.add(cb.equal(root.get("currency"), currency));
                    break;
                }
            }

            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Listing> notHiddenByUser(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.conjunction();
            }

            Subquery<Long> hiddenListingIds = query.subquery(Long.class);
            var hidden = hiddenListingIds.from(HiddenListing.class);
            hiddenListingIds.select(hidden.get("id").get("listingId"))
                .where(cb.equal(hidden.get("id").get("userId"), userId));

            return cb.not(root.get("id").in(hiddenListingIds));
        };
    }

    public static Specification<Listing> hasAreaInRange(Float areaFrom, Float areaTo) {
        return (root, query, cb) -> {
            if ((areaFrom == null || areaFrom <= 0) && (areaTo == null || areaTo <= 0)) {
                return cb.conjunction();
            }

            // Для комнат площадь сравнивается по площади самой комнаты (roomArea), для квартир —
            // по общей площади (areaTotal); roomArea заполнено только у категории ROOM.
            var effectiveArea = cb.coalesce(root.<Float>get("roomArea"), root.<Float>get("areaTotal"));

            if (areaFrom != null && areaFrom > 0 && areaTo != null && areaTo > 0) {
                return cb.between(effectiveArea, areaFrom, areaTo);
            } else if (areaFrom != null && areaFrom > 0) {
                return cb.greaterThanOrEqualTo(effectiveArea, areaFrom);
            } else {
                return cb.lessThanOrEqualTo(effectiveArea, areaTo);
            }
        };
    }
}