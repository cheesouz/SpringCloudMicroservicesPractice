package com.cheesouz.api_service.exception;

import java.util.List;

public record ErrorResponse(String message, List<FieldError> errors) {

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, List.of());
    }

    public record FieldError(String field, String message) {
    }
}