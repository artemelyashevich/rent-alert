package com.rentalert.common.exception;

import org.springframework.http.HttpStatus;

public class AppAlreadyExistsException extends AppException {

    public AppAlreadyExistsException(String message) {
        super(InternalExceptionCode.EX409, message, HttpStatus.CONFLICT);
    }
}