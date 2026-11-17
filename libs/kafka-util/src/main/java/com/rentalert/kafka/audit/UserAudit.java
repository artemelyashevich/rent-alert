package com.rentalert.kafka.audit;

import com.rentalert.dto.event.user.UserEvent;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method as a user action to be recorded in the ops audit log. On successful completion the
 * aspect publishes a {@link com.rentalert.dto.event.UserActionEvent} through the outbox, so the event is written in
 * the business transaction and is not published when the method throws.
 * <p>
 * SpEL expressions are evaluated against the method parameters by name. {@link #params()} are evaluated before the
 * method runs; {@link #filterId()} is evaluated after it runs and can reference the return value as {@code #result}.
 * Calls from within the same bean bypass the proxy and are not audited.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserAudit {

    UserEvent value();

    /**
     * SpEL expression for the user id, evaluated against the method parameters. When blank, the id is taken from
     * the {@link CurrentUserIdProvider} bean.
     */
    String userId() default "";

    /**
     * SpEL expression for the filter id, evaluated after a successful call against the method parameters and
     * {@code #result}. When blank, no filter id is recorded.
     */
    String filterId() default "";

    /**
     * SpEL expressions evaluated against the method parameters before the call. Values are passed to the audit
     * log in order and fill the {@code %s} placeholders of the {@link UserEvent} description.
     */
    String[] params() default {};
}
