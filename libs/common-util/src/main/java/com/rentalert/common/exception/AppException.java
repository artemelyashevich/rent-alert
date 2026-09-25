package com.rentalert.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final InternalExceptionCode errorCode;

    public AppException() {
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        this.errorCode = InternalExceptionCode.EX500;
    }

    public AppException(InternalExceptionCode errorCode, HttpStatus httpStatus) {
        super(errorCode.getLabel());
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public AppException(InternalExceptionCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public AppException(InternalExceptionCode errorCode, Throwable cause, HttpStatus httpStatus) {
        super(errorCode.getLabel(), cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public AppException(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
        this.errorCode = InternalExceptionCode.EX500;
    }

    public AppException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = InternalExceptionCode.EX500;
    }

    public AppException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = InternalExceptionCode.EX500;
    }

    public AppException(Throwable cause, HttpStatus httpStatus) {
        super(cause);
        this.httpStatus = httpStatus;
        this.errorCode = InternalExceptionCode.EX500;
    }

    public AppException(Throwable cause) {
        super(cause);
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        this.errorCode = InternalExceptionCode.EX500;
    }

    public AppException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, HttpStatus httpStatus) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.httpStatus = httpStatus;
        this.errorCode = InternalExceptionCode.EX500;
    }
}