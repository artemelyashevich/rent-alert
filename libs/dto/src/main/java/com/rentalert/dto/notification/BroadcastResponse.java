package com.rentalert.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastResponse {

    private Long broadcastId;
    private int totalRecipients;
    private int sent;
    private int failed;
    private int skipped;
}
