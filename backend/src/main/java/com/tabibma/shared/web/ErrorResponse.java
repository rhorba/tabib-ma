package com.tabibma.shared.web;

import java.util.List;

public record ErrorResponse(ErrorBody error) {

    public record ErrorBody(String code, String message, List<FieldError> details, String requestId) {
    }

    public record FieldError(String field, String message) {
    }
}
