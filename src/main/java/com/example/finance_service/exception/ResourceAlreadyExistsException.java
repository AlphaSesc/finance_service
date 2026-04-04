package com.example.finance_service.exception;

import org.springframework.http.HttpStatus;

public class ResourceAlreadyExistsException extends BusinessException {

    public ResourceAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}