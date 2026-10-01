package com.rentalert.ops_ms.repository.projection;

public interface UserActivityCountProjection {

    Long getUserId();

    long getActionCount();
}
