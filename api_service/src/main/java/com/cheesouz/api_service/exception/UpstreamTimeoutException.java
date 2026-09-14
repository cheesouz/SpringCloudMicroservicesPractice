package com.cheesouz.api_service.exception;

public class UpstreamTimeoutException extends RuntimeException {

    public UpstreamTimeoutException(String message) {
        super(message);
    }
}