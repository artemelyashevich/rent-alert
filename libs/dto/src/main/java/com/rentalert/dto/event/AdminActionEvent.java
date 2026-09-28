package com.rentalert.dto.event;

import com.rentalert.dto.event.admin.AdminEvent;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminActionEvent extends AbstractEvent {

    private AdminEvent event;
    private List<String> params;

    @Override
    public EventInitiatorType getEventType() {
        return EventInitiatorType.ADMIN;
    }
}
