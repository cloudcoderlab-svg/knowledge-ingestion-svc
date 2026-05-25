package com.kengine.ingestion.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
    log.error("Unhandled exception", ex);

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("error", ex.getClass().getSimpleName());
    errorResponse.put("message", ex.getMessage());
    errorResponse.put("details", ex.toString());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
      IllegalArgumentException ex) {
    log.error("IllegalArgumentException", ex);

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("error", "BadRequest");
    errorResponse.put("message", ex.getMessage());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationException(
      MethodArgumentNotValidException ex) {
    log.error("Validation failed", ex);

    Map<String, String> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "",
                    (existing, replacement) -> existing));

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("error", "ValidationError");
    errorResponse.put("message", "Validation failed");
    errorResponse.put("fieldErrors", fieldErrors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }
}
