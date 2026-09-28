package com.rentalert.identity_ms.mapper;

import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.dto.profile.UserProfileCreateDto;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.dto.profile.UserProfileUpdateDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserProfileMapper {

    @Mapping(source = "settings.botStatus", target = "botStatus")
    @Mapping(source = "settings.quietFrom", target = "quietFrom")
    @Mapping(source = "settings.quietTo", target = "quietTo")
    UserProfileCreateDto toDto(UserProfile entity);

    @Mapping(target = "telegramId", ignore = true)
    @Mapping(target = "telegramIdHash", ignore = true)
    @Mapping(target = "settings", ignore = true)
    UserProfile toEntity(UserProfileCreateDto requestDto);

    @Mapping(source = "botStatus", target = "settings.botStatus")
    @Mapping(source = "quietFrom", target = "settings.quietFrom")
    @Mapping(source = "quietTo", target = "settings.quietTo")
    void updateEntityFromDto(UserProfileUpdateDto requestDto, @MappingTarget UserProfile entity);

    @Mapping(source = "id", target = "userId")
    @Mapping(source = "createdAt", target = "registeredAt")
    @Mapping(source = "settings.botStatus", target = "botStatus")
    @Mapping(source = "settings.roleName", target = "roleName")
    @Mapping(source = "settings.quietFrom", target = "quietFrom")
    @Mapping(source = "settings.quietTo", target = "quietTo")
    @Mapping(source = "settings.email", target = "email")
    @Mapping(source = "settings.phoneNumber", target = "phoneNumber")
    @Mapping(source = "settings.colorTheme", target = "colorTheme")
    @Mapping(source = "settings.language", target = "language")
    @Mapping(target = "activeFilters", expression = "java(java.util.List.of())")
    @Mapping(target = "recentFeedbacks", expression = "java(java.util.List.of())")
    UserProfileResponse toResponse(UserProfile entity);
}
