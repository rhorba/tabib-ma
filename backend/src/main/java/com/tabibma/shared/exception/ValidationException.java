package com.tabibma.shared.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, message);
    }
}
