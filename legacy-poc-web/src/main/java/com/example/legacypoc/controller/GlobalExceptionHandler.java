package com.example.legacypoc.controller;

import com.example.legacypoc.exception.EmployeeNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(assignableTypes = EmployeeController.class)
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmployeeNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleEmployeeNotFound(EmployeeNotFoundException exception, Model model) {
        LOGGER.warn("Employee not found", exception);
        model.addAttribute("errorTitle", "Employee Not Found");
        model.addAttribute("errorMessage", "The requested employee could not be found.");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpectedException(Exception exception, Model model) {
        LOGGER.error("Unexpected application error", exception);
        model.addAttribute("errorTitle", "Application Error");
        model.addAttribute("errorMessage",
                "The application could not complete your request. Please contact support if the problem continues.");
        return "error";
    }
}
