package com.rentalert.dto.statistics;

import com.rentalert.dto.event.user.UserEvent;
import java.time.LocalDate;

public record UserActivityLog(UserEvent eventType, LocalDate eventDate, Integer count) {

}
