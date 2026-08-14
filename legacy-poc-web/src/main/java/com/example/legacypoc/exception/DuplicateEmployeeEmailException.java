package com.example.legacypoc.exception;

public class DuplicateEmployeeEmailException extends RuntimeException {
    public DuplicateEmployeeEmailException(String email) {
        super("An employee with this email already exists: " + email);
    }
}
