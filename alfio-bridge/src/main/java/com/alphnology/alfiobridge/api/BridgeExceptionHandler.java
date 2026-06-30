package com.alphnology.alfiobridge.api;

import com.alphnology.alfiobridge.lookup.ReferenceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class BridgeExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BridgeExceptionHandler.class);

    @ExceptionHandler(ReferenceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ReferenceNotFoundException ex) {
        log.info("Alfio bridge lookup returned no result: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("not_found", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Alfio bridge request validation failed: {}", ex.getMessage());
        Map<String, Object> body = errorBody("validation_error", "The request is invalid.");
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fields.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Alfio bridge rejected request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(errorBody("validation_error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unexpected alfio bridge failure", ex);
        return ResponseEntity.internalServerError().body(errorBody("internal_error", "Unexpected bridge failure."));
    }

    private Map<String, Object> errorBody(String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("error", error);
        body.put("message", message);
        return body;
    }
}
