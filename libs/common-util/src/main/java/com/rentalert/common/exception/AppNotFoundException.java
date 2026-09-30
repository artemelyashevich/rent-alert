package com.rentalert.common.exception;

import org.springframework.http.HttpStatus;

public class AppNotFoundException extends AppException {

    public AppNotFoundException(String message) {
        super(InternalExceptionCode.EX404, message, HttpStatus.NOT_FOUND);
    }

    public AppNotFoundException(InternalExceptionCode errorCode) {
        super(errorCode, HttpStatus.NOT_FOUND);
    }

    public AppNotFoundException() {
        super(InternalExceptionCode.EX404, HttpStatus.NOT_FOUND);
    }
}