package com.victadore.webmafia.mafia_web_of_lies.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GameException.class)
    public ResponseEntity<?> handleGameException(GameException e) {
        Map<String, String> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "An unexpected error occurred: " + e.getMessage());
        return ResponseEntity.internalServerError().body(error);
    }
}