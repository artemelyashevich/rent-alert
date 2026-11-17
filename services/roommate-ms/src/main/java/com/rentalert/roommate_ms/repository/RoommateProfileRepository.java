package com.rentalert.roommate_ms.repository;

import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.roommate_ms.domain.Gender;
import com.rentalert.roommate_ms.domain.RoommateProfile;
import com.rentalert.roommate_ms.domain.RoommateScenario;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoommateProfileRepository extends JpaRepository<RoommateProfile, Long> {

    @Query("""
        select c from RoommateProfile c
        where c.userId <> :viewerId
          and c.active = true
          and c.searchable = true
          and c.bioModStatus in (
              com.rentalert.dto.roommate.ModerationStatus.APPROVED_AUTO,
              com.rentalert.dto.roommate.ModerationStatus.APPROVED)
          and lower(c.city) = lower(:city)
          and (cast(:budgetMin as Integer) is null or c.budgetMax is null or c.budgetMax >= :budgetMin)
          and (cast(:budgetMax as Integer) is null or c.budgetMin is null or c.budgetMin <= :budgetMax)
          and (cast(:moveFrom as LocalDate) is null or c.moveInTo is null or c.moveInTo >= :moveFrom)
          and (cast(:moveTo as LocalDate) is null or c.moveInFrom is null or c.moveInFrom <= :moveTo)
          and c.gender in :viewerPref
          and array_contains(c.genderPref, :viewerGender)
          and not exists (
              select 1 from RoommateHidden h
              where h.id.userId = :viewerId
                and h.id.subjectType = com.rentalert.roommate_ms.domain.RoommateHiddenSubjectType.ROOMMATE_PROFILE
                and h.id.subjectId = c.userId)
          and not exists (
              select 1 from RoommateMatch m
              where m.scenario = :scenario
                and ((m.userLoId = :viewerId and m.userHiId = c.userId)
                  or (m.userLoId = c.userId and m.userHiId = :viewerId)))
        order by c.createdAt desc, c.userId desc
        """)
    List<RoommateProfile> findCandidatePool(
        @Param("viewerId") Long viewerId,
        @Param("city") String city,
        @Param("budgetMin") Integer budgetMin,
        @Param("budgetMax") Integer budgetMax,
        @Param("moveFrom") LocalDate moveFrom,
        @Param("moveTo") LocalDate moveTo,
        @Param("viewerPref") List<Gender> viewerPref,
        @Param("viewerGender") String viewerGender,
        @Param("scenario") RoommateScenario scenario,
        Pageable pageable);

    List<RoommateProfile> findAllByActiveTrueAndSearchableTrueAndBioModStatusIn(List<ModerationStatus> bioModStatuses);

    @Query("""
        select count(c) from RoommateProfile c
        where c.userId <> :viewerId
          and c.active = true
          and c.searchable = true
          and c.bioModStatus in (
              com.rentalert.dto.roommate.ModerationStatus.APPROVED_AUTO,
              com.rentalert.dto.roommate.ModerationStatus.APPROVED)
          and lower(c.city) = lower(:city)
          and (cast(:budgetMin as Integer) is null or c.budgetMax is null or c.budgetMax >= :budgetMin)
          and (cast(:budgetMax as Integer) is null or c.budgetMin is null or c.budgetMin <= :budgetMax)
          and (cast(:moveFrom as LocalDate) is null or c.moveInTo is null or c.moveInTo >= :moveFrom)
          and (cast(:moveTo as LocalDate) is null or c.moveInFrom is null or c.moveInFrom <= :moveTo)
          and c.gender in :viewerPref
          and array_contains(c.genderPref, :viewerGender)
          and c.createdAt > :since
          and not exists (
              select 1 from RoommateHidden h
              where h.id.userId = :viewerId
                and h.id.subjectType = com.rentalert.roommate_ms.domain.RoommateHiddenSubjectType.ROOMMATE_PROFILE
                and h.id.subjectId = c.userId)
          and not exists (
              select 1 from RoommateMatch m
              where m.scenario = :scenario
                and ((m.userLoId = :viewerId and m.userHiId = c.userId)
                  or (m.userLoId = c.userId and m.userHiId = :viewerId)))
        """)
    long countFreshCandidates(
        @Param("viewerId") Long viewerId,
        @Param("city") String city,
        @Param("budgetMin") Integer budgetMin,
        @Param("budgetMax") Integer budgetMax,
        @Param("moveFrom") LocalDate moveFrom,
        @Param("moveTo") LocalDate moveTo,
        @Param("viewerPref") List<Gender> viewerPref,
        @Param("viewerGender") String viewerGender,
        @Param("scenario") RoommateScenario scenario,
        @Param("since") Instant since);
}
