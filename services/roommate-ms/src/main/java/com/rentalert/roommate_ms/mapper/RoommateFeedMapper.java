package com.rentalert.roommate_ms.mapper;

import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.roommate_ms.domain.RoommateListing;
import com.rentalert.roommate_ms.domain.RoommateProfile;
import com.rentalert.roommate_ms.dto.RoommateCandidateView;
import com.rentalert.roommate_ms.dto.RoommateListingCardView;
import com.rentalert.roommate_ms.util.RoommateAgeUtil;
import java.util.Arrays;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    imports = RoommateAgeUtil.class
)
public interface RoommateFeedMapper {

    @Mapping(target = "userId", source = "candidate.userId")
    @Mapping(target = "username", source = "author.username")
    @Mapping(target = "photoUrl", source = "author.photoUrl")
    @Mapping(target = "ageRange", expression = "java(RoommateAgeUtil.ageRange(candidate.getBirthYear()))")
    @Mapping(target = "city", source = "candidate.city")
    @Mapping(target = "budgetMin", source = "candidate.budgetMin")
    @Mapping(target = "budgetMax", source = "candidate.budgetMax")
    @Mapping(target = "currency", source = "candidate.currency")
    @Mapping(target = "moveInFrom", source = "candidate.moveInFrom")
    @Mapping(target = "moveInTo", source = "candidate.moveInTo")
    @Mapping(target = "lifestyleTags", expression = "java(toList(candidate.getLifestyleTags()))")
    @Mapping(target = "sharedTags", source = "sharedTags")
    @Mapping(target = "schedule", source = "candidate.schedule")
    @Mapping(target = "smoking", source = "candidate.smoking")
    @Mapping(target = "pets", source = "candidate.pets")
    @Mapping(target = "guests", source = "candidate.guests")
    @Mapping(target = "bio", source = "candidate.bioApproved")
    @Mapping(target = "score", source = "score")
    RoommateCandidateView toCandidateView(RoommateProfile candidate, UserBriefResponse author, int score, List<String> sharedTags);

    @Mapping(target = "id", source = "listing.id")
    @Mapping(target = "authorUserId", source = "listing.authorUserId")
    @Mapping(target = "authorUsername", source = "author.username")
    @Mapping(target = "authorPhotoUrl", source = "author.photoUrl")
    @Mapping(target = "city", source = "listing.city")
    @Mapping(target = "district", source = "listing.district")
    @Mapping(target = "metroStation", source = "listing.metroStation")
    @Mapping(target = "addressPublic", source = "listing.addressPublic")
    @Mapping(target = "geoLat", source = "listing.geoLat")
    @Mapping(target = "geoLng", source = "listing.geoLng")
    @Mapping(target = "rentShare", source = "listing.rentShare")
    @Mapping(target = "currency", source = "listing.currency")
    @Mapping(target = "roomCount", source = "listing.roomCount")
    @Mapping(target = "flatmatesCount", source = "listing.flatmatesCount")
    @Mapping(target = "availableFrom", source = "listing.availableFrom")
    @Mapping(target = "houseRules", expression = "java(toList(listing.getHouseRules()))")
    @Mapping(target = "description", source = "listing.descriptionApproved")
    @Mapping(target = "genderPref", expression = "java(toList(listing.getGenderPref()))")
    @Mapping(target = "ageMin", source = "listing.ageMin")
    @Mapping(target = "ageMax", source = "listing.ageMax")
    @Mapping(target = "score", source = "score")
    RoommateListingCardView toListingCardView(RoommateListing listing, UserBriefResponse author, int score);

    default List<String> toList(String[] array) {
        return array == null ? List.of() : Arrays.asList(array);
    }
}
