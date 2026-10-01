package com.rentalert.notification_ms.service;

import com.rentalert.dto.event.ServiceStartedEvent;

public interface AppReadinessService {

    /**
     * Records that a service has started. Once every service from {@code app.startup.services} has reported within
     * the startup window, sends the admin a single {@code APP_STARTED} notification and starts collecting anew.
     * Reports older than the window are ignored.
     */
    void markStarted(ServiceStartedEvent event);
}
