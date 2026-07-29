package com.tabibma.shared.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", HttpStatus.FORBIDDEN, message);
    }
}
