package com.rentalert.dto.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationDto {

    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private NotificationType type;
    private NotificationEngine engine;
    private Long userId;
    private String receiver;
    private String deduplicationKey;
    private String content;
    private Map<String, String> templateParams;
    private Long listingId;
    private String templateName;
    private boolean pin;
}