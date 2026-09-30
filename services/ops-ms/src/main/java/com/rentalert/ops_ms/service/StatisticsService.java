package com.rentalert.ops_ms.service;

import com.rentalert.dto.event.UserActionEvent;

public interface StatisticsService {

    void record(UserActionEvent event);
}
