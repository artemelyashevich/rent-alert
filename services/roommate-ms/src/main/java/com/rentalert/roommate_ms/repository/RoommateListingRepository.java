package com.rentalert.roommate_ms.repository;

import com.rentalert.roommate_ms.domain.RoommateListing;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoommateListingRepository extends JpaRepository<RoommateListing, Long> {

    Page<RoommateListing> findAllByAuthorUserId(Long authorUserId, Pageable pageable);

    @Query("""
        select rl from RoommateListing rl
        where rl.status = com.rentalert.roommate_ms.domain.RoommateListingStatus.ACTIVE
          and rl.descriptionModStatus in (
              com.rentalert.dto.roommate.ModerationStatus.APPROVED_AUTO,
              com.rentalert.dto.roommate.ModerationStatus.APPROVED)
          and rl.authorUserId <> :viewerId
          and lower(rl.city) = lower(:city)
          and (cast(:budgetMax as Integer) is null or rl.rentShare is null or rl.rentShare <= :budgetMax)
          and (cast(:budgetMin as Integer) is null or rl.rentShare is null or rl.rentShare >= :budgetMin)
          and array_contains(rl.genderPref, :viewerGender)
          and (cast(:viewerAge as Integer) is null or rl.ageMin is null or rl.ageMin <= :viewerAge)
          and (cast(:viewerAge as Integer) is null or rl.ageMax is null or rl.ageMax >= :viewerAge)
          and not exists (
              select 1 from RoommateHidden h
              where h.id.userId = :viewerId
                and h.id.subjectType = com.rentalert.roommate_ms.domain.RoommateHiddenSubjectType.ROOMMATE_LISTING
                and h.id.subjectId = rl.id)
          and not exists (
              select 1 from RoommateMatch m
              where m.scenario = com.rentalert.roommate_ms.domain.RoommateScenario.ROOM
                and ((m.userLoId = :viewerId and m.userHiId = rl.authorUserId)
                  or (m.userLoId = rl.authorUserId and m.userHiId = :viewerId)))
        order by rl.createdAt desc, rl.id desc
        """)
    List<RoommateListing> findFeedPool(
        @Param("viewerId") Long viewerId,
        @Param("city") String city,
        @Param("budgetMin") Integer budgetMin,
        @Param("budgetMax") Integer budgetMax,
        @Param("viewerGender") String viewerGender,
        @Param("viewerAge") Integer viewerAge,
        Pageable pageable);
}
