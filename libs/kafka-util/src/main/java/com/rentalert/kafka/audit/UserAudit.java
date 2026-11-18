package com.rentalert.kafka.audit;

import com.rentalert.dto.event.user.UserEvent;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserAudit {

    UserEvent value();

    String userId() default "";

    String filterId() default "";

    String[] params() default {};
}
