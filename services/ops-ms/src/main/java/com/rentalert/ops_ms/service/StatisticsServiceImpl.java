package com.rentalert.ops_ms.service;

import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.ops_ms.repository.UserStatisticsRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final UserStatisticsRepository userStatisticsRepository;
    private final ZoneId zone;

    public StatisticsServiceImpl(UserStatisticsRepository userStatisticsRepository,
                                 @Value("${app.bot.timezone:Europe/Minsk}") ZoneId zone) {
        this.userStatisticsRepository = userStatisticsRepository;
        this.zone = zone;
    }

    @Override
    @Transactional
    public void record(UserActionEvent event) {
        if (event.getInitiatorId() == null) {
            return;
        }
        userStatisticsRepository.increment(event.getInitiatorId(), event.getFilterId(), event.getEvent().name(),
            LocalDate.now(zone));
    }
}
