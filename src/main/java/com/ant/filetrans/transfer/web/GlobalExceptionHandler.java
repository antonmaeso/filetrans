package com.ant.filetrans.transfer.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for REST controllers.
 * Handles validation errors and mapper conversion errors.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles Jakarta validation errors from @Valid annotation.
     * Provides detailed error messages including field name, constraint, and expected format.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        
        var fieldError = ex.getBindingResult().getFieldError();
        String fieldName = fieldError != null ? fieldError.getField() : "unknown";
        String errorMessage = fieldError != null ? fieldError.getDefaultMessage() : "Validation failed";
        String rejectedValue = fieldError != null && fieldError.getRejectedValue() != null 
            ? fieldError.getRejectedValue().toString() 
            : "null";
        
        // Build descriptive message that includes field name and constraint
        String detailedMessage = String.format("Validation failed for field '%s': %s", fieldName, errorMessage);
        if (rejectedValue.equals("null") && errorMessage.contains("must not be null")) {
            detailedMessage = String.format("Field '%s' is required and must not be null", fieldName);
        }
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", detailedMessage);
        body.put("field", fieldName);
        
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Handles IllegalArgumentException from mapper validation.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid request: {}", ex.getMessage());
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        
        return ResponseEntity.badRequest().body(body);
    }
}
