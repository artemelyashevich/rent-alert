package com.rentalert.processing_ms.repository.projection;

public interface CityListingCountProjection {

    String getCity();

    long getTotal();

    long getActive();
}
