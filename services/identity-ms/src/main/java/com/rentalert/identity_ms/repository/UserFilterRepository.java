package com.rentalert.identity_ms.repository;

import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.listing.ListingCategory;
import com.rentalert.identity_ms.domain.UserFilter;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFilterRepository extends JpaRepository<@NonNull UserFilter, @NonNull Long> {

    List<UserFilter> findAllByUserProfileId(Long userProfileId);

    Optional<UserFilter> findByIdAndUserProfileId(Long id, Long userProfileId);

    Optional<UserFilter> findByOwnerMatchId(Long ownerMatchId);

    int countByUserProfileId(Long userProfileId);

    Optional<UserFilter> findByUserProfileIdAndActiveTrue(Long userProfileId);

    List<UserFilter> findAllByActive(boolean active);

    @EntityGraph(attributePaths = {"userProfile", "userProfile.settings"})
    @Query("""
            select uf
            from UserFilter uf
            where uf.active = true
              and uf.currency = :currency
              and (
                  (uf.priceFrom is null and uf.priceTo is null)
                  or cast(:price as Integer) is null
                  or (
                      (uf.priceFrom is null or uf.priceFrom <= :price)
                      and (uf.priceTo is null or uf.priceTo >= :price)
                  )
              )
              and (
                  (uf.roomsFrom is null and uf.roomsTo is null)
                  or cast(:rooms as Integer) is null
                  or (
                      (uf.roomsFrom is null or uf.roomsFrom <= :rooms)
                      and (uf.roomsTo is null or uf.roomsTo >= :rooms)
                  )
              )
              and (
                  (uf.areaFrom is null and uf.areaTo is null)
                  or cast(:area as Float) is null
                  or (
                      (uf.areaFrom is null or uf.areaFrom <= :area)
                      and (uf.areaTo is null or uf.areaTo >= :area)
                  )
              )
            """)
    List<UserFilter> findMatchingFilters(
        @Param("price") Integer price,
        @Param("rooms") Integer rooms,
        @Param("area") Float area,
        @Param("currency") Currency currency);

    @EntityGraph(attributePaths = {"userProfile", "userProfile.settings"})
    @Query("""
            select uf
            from UserFilter uf
            where uf.active = true
              and uf.userProfile is not null
              and (uf.category is null or uf.category = :category)
              and (cast(:city as String) is null or lower(uf.city) = lower(:city))
              and (
                  (uf.roomsFrom is null and uf.roomsTo is null)
                  or cast(:rooms as Integer) is null
                  or (
                      (uf.roomsFrom is null or uf.roomsFrom <= :rooms)
                      and (uf.roomsTo is null or uf.roomsTo >= :rooms)
                  )
              )
              and (
                  (uf.areaFrom is null and uf.areaTo is null)
                  or cast(:area as Float) is null
                  or (
                      (uf.areaFrom is null or uf.areaFrom <= :area)
                      and (uf.areaTo is null or uf.areaTo >= :area)
                  )
              )
            """)
    List<UserFilter> findActiveFiltersByRoomsAndArea(
        @Param("rooms") Integer rooms,
        @Param("area") Float area,
        @Param("city") String city,
        @Param("category") ListingCategory category);
}