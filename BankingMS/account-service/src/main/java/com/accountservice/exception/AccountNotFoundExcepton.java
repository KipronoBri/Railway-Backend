package com.accountservice.exception;

public class AccountNotFoundExcepton extends RuntimeException {
    public AccountNotFoundExcepton(String message) {
        super(message);
    }
}
