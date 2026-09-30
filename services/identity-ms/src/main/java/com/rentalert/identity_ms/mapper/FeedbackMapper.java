package com.rentalert.identity_ms.mapper;


import com.rentalert.dto.profile.FeedbackResponse;
import com.rentalert.identity_ms.domain.Feedback;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FeedbackMapper {

    @Mapping(target = "userId", source = "userProfile.id")
    @Mapping(target = "username", source = "userProfile.username")
    FeedbackResponse toResponse(Feedback feedback);
}
