package com.mag.gateway.config.web;

import com.mag.gateway.config.ConfigNotFoundException;
import com.mag.gateway.config.ConfigValidationException;
import com.mag.gateway.config.FieldError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

/** Maps config exceptions to HTTP: 400 with {"errors":[{field,reason}]}, 404 for unknown ids. */
@RestControllerAdvice(assignableTypes = AdminController.class)
public class AdminExceptionHandler {

    @ExceptionHandler(ConfigValidationException.class)
    public ResponseEntity<Map<String, List<FieldError>>> invalid(ConfigValidationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("errors", e.errors()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, List<FieldError>>> malformed(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("errors", List.of(new FieldError("body", "malformed JSON or wrong field type"))));
    }

    @ExceptionHandler(ConfigNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(ConfigNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
