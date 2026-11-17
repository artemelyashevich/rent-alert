package com.rentalert.kafka.audit;

/**
 * Supplies the id of the user on whose behalf the current call runs, used by {@link UserAudit} when the annotation
 * does not define a {@code userId} expression. Implemented by each service, which knows how its security context
 * is resolved.
 */
@FunctionalInterface
public interface CurrentUserIdProvider {

    /**
     * @return the current user's id, or {@code null} when no user is authenticated
     */
    Long currentUserId();
}
