package com.victadore.webmafia.mafia_web_of_lies.exception;

import lombok.Getter;
import java.util.List;
import java.util.Map;

@Getter
public class ValidationException extends RuntimeException {
    private final Map<String, List<String>> fieldErrors;
    
    public ValidationException(String message, Map<String, List<String>> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }
    
    public ValidationException(String message) {
        super(message);
        this.fieldErrors = null;
    }
} 