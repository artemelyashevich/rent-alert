package com.rentalert.processing_ms.repository.projection;

import com.rentalert.dto.listing.ScheduleProvider;
import java.time.Instant;

public interface ProviderLastParsedProjection {

    ScheduleProvider getProvider();

    Instant getLastParsedAt();
}
