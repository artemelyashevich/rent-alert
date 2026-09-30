package com.rentalert.common.exception;

import org.springframework.http.HttpStatus;

public class UserBlockedException extends AppException {

    public UserBlockedException() {
        super(InternalExceptionCode.EX403_USER_BLOCKED, HttpStatus.FORBIDDEN);
    }
}
