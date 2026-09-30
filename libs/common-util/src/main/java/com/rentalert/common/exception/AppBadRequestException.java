package com.rentalert.common.exception;

import org.springframework.http.HttpStatus;

public class AppBadRequestException extends AppException {

    public AppBadRequestException(String message) {
        super(InternalExceptionCode.EX400, message, HttpStatus.BAD_REQUEST);
    }

    public AppBadRequestException(InternalExceptionCode errorCode) {
        super(errorCode, HttpStatus.BAD_REQUEST);
    }

    public AppBadRequestException() {
        super(InternalExceptionCode.EX400, HttpStatus.BAD_REQUEST);
    }
}