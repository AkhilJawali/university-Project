package com.utms.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/campuses");
    }

    @Test
    void handleValidationException_returns400WithFieldDetail() {
        ValidationException ex = new ValidationException("campusId", "Campus not found", 999L);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("status")).isEqualTo(400);
        assertThat(response.getBody().get("message")).isEqualTo("Validation failed");
    }

    @Test
    void handleNotFound_returns404() {
        EntityNotFoundException ex = new EntityNotFoundException("Campus", 999L);

        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("status")).isEqualTo(404);
        assertThat(response.getBody().get("message")).asString().contains("Campus not found");
    }

    @Test
    void handleConflict_returns409() {
        ConflictException ex = new ConflictException("Cannot delete campus: 3 active departments");

        ResponseEntity<Map<String, Object>> response = handler.handleConflict(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("status")).isEqualTo(409);
        assertThat(response.getBody().get("message")).asString().contains("3 active departments");
    }

    @Test
    void handleAccessDenied_returns403() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");

        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("message")).isEqualTo("Access denied");
    }

    @Test
    void handleGeneral_returns500WithGenericMessage() {
        Exception ex = new RuntimeException("Unexpected DB connection error");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().get("message")).isEqualTo("An unexpected error occurred");
        // Must NOT expose internal details
        assertThat(response.getBody().get("message").toString()).doesNotContain("DB connection");
    }

    @Test
    void errorResponse_includesPathAndTimestamp() {
        ConflictException ex = new ConflictException("test conflict");

        ResponseEntity<Map<String, Object>> response = handler.handleConflict(ex, webRequest);

        assertThat(response.getBody().get("path")).isEqualTo("/api/v1/campuses");
        assertThat(response.getBody().get("timestamp")).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Conflict");
    }
}
