package com.rentalert.common.exception;

import org.springframework.http.HttpStatus;

public class NoActiveFiltersException extends AppException {

    public NoActiveFiltersException() {
        super(InternalExceptionCode.EX404_NO_ACTIVE_FILTERS, HttpStatus.NOT_FOUND);
    }

    public NoActiveFiltersException(String message) {
        super(InternalExceptionCode.EX404_NO_ACTIVE_FILTERS, message, HttpStatus.NOT_FOUND);
    }
}
