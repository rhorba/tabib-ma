package com.tabibma.shared.exception;

import com.tabibma.shared.web.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        String requestId = requestId(request);
        log.warn("API exception [{}] code={} path={}", requestId, ex.getCode(), request.getRequestURI(), ex);
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse(new ErrorResponse.ErrorBody(ex.getCode(), ex.getMessage(), List.of(), requestId)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String requestId = requestId(request);
        List<ErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(new ErrorResponse.ErrorBody("VALIDATION_FAILED",
                        "The request contains invalid fields.", details, requestId)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String requestId = requestId(request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(new ErrorResponse.ErrorBody("FORBIDDEN",
                        "You do not have permission to access this resource.", List.of(), requestId)));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        String requestId = requestId(request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(new ErrorResponse.ErrorBody("UNAUTHORIZED",
                        "Authentication is required.", List.of(), requestId)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        String requestId = requestId(request);
        log.error("Unexpected error [{}] path={}", requestId, request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(new ErrorResponse.ErrorBody("INTERNAL_ERROR",
                        "An unexpected error occurred.", List.of(), requestId)));
    }

    private String requestId(HttpServletRequest request) {
        Object attr = request.getAttribute("requestId");
        return attr != null ? attr.toString() : UUID.randomUUID().toString();
    }
}
