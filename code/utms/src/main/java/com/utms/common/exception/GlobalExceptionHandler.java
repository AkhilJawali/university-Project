package com.utms.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        List<Map<String, Object>> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("field", error.getField());
                    detail.put("message", error.getDefaultMessage());
                    detail.put("rejectedValue", error.getRejectedValue());
                    return detail;
                })
                .collect(Collectors.toList());

        Map<String, Object> body = buildErrorBody(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex, WebRequest request) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("field", ex.getField());
        detail.put("message", ex.getMessage());
        detail.put("rejectedValue", ex.getRejectedValue());

        Map<String, Object> body = buildErrorBody(HttpStatus.BAD_REQUEST, "Validation failed", request, List.of(detail));
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex, WebRequest request) {
        Map<String, Object> body = buildErrorBody(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex, WebRequest request) {
        Map<String, Object> body = buildErrorBody(HttpStatus.CONFLICT, ex.getMessage(), request, null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", request.getDescription(false));
        Map<String, Object> body = buildErrorBody(HttpStatus.FORBIDDEN, "Access denied", request, null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex, WebRequest request) {
        log.error("Unhandled exception at {}: {}", request.getDescription(false), ex.getMessage(), ex);
        Map<String, Object> body = buildErrorBody(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred", request, null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private Map<String, Object> buildErrorBody(HttpStatus status, String message,
                                                WebRequest request, List<Map<String, Object>> details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        if (details != null && !details.isEmpty()) {
            body.put("details", details);
        }
        return body;
    }
}
