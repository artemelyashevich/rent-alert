package com.rentalert.dto.event;

import com.rentalert.dto.event.user.UserEvent;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A user action for the ops audit log, sent to {@link KafkaTopics#USER_EVENTS} with key = userId. The user is {@link #getInitiatorId()}.
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserActionEvent extends AbstractEvent {

    private UserEvent event;
    private Long filterId;
    private List<String> params;

    @Override
    public EventInitiatorType getEventType() {
        return EventInitiatorType.USER;
    }
}
