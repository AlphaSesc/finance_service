package com.example.finance_service.exception;

import org.springframework.http.HttpStatus;

public class InvalidRequestException extends BusinessException {

    public InvalidRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}