package com.rentalert.listing_ms.repository;

import com.rentalert.listing_ms.domain.Favorite;
import com.rentalert.listing_ms.domain.ListingStatusName;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class FavoriteSpecification {

    private FavoriteSpecification() {
    }

    public static Specification<Favorite> hasUserId(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.disjunction();
            }
            Class<?> resultType = query.getResultType();
            if (resultType != Long.class && resultType != long.class) {
                root.fetch("listing", JoinType.LEFT);
                root.fetch("listingStatus", JoinType.LEFT);
            }
            return cb.equal(root.get("id").get("userId"), userId);
        };
    }

    public static Specification<Favorite> hasStatus(ListingStatusName status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.join("listingStatus").get("name"), status);
        };
    }
}
