package com.rentalert.kafka.audit;

@FunctionalInterface
public interface CurrentUserIdProvider {

    Long currentUserId();
}
