package com.rentalert.identity_ms.mapper;


import com.rentalert.identity_ms.domain.Feedback;
import com.rentalert.identity_ms.dto.FeedbackResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FeedbackMapper {

    @Mapping(target = "userId", source = "userProfile.id")
    @Mapping(target = "username", source = "userProfile.username")
    FeedbackResponse toResponse(Feedback feedback);
}
