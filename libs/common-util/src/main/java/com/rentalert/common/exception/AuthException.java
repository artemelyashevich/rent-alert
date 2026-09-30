package com.rentalert.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthException extends AppException {

    public AuthException(String message) {
        super(InternalExceptionCode.EX401, message, HttpStatus.UNAUTHORIZED);
    }

    public AuthException() {
        super(InternalExceptionCode.EX401, HttpStatus.UNAUTHORIZED);
    }
}