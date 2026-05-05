package com.clinicadigital.gateway.exception;

import com.clinicadigital.iam.application.UserContextService;
import com.clinicadigital.tenant.application.QuotaExceededException;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MeterRegistry meterRegistry;

    public GlobalExceptionHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @ExceptionHandler({TenantContextMissingException.class, InvalidTenantContextException.class})
    public ResponseEntity<Map<String, Object>> handleTenantContext(RuntimeException exception) {
        meterRegistry.counter(
                "tenant.isolation.failures",
                "reason", tenantFailureReason(exception),
                "tenant_id", currentTenantId())
                .increment();
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(operationOutcome("forbidden", exception.getMessage()));
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuotaExceeded(QuotaExceededException exception) {
        meterRegistry.counter(
                "tenant.quota.blocks",
            "metric", exception.getMetric(),
            "tenant_id", exception.getTenantId().toString())
                .increment();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(operationOutcome("throttled", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(IllegalArgumentException exception,
                                                                HttpServletRequest request) {
        if (request != null && request.getRequestURI() != null && request.getRequestURI().startsWith("/auth/")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(operationOutcome("forbidden", exception.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(operationOutcome("invalid", exception.getMessage()));
    }

    @ExceptionHandler(AuthSessionException.class)
    public ResponseEntity<Map<String, Object>> handleAuthSession(AuthSessionException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(operationOutcome("forbidden", exception.getMessage()));
    }

    @ExceptionHandler(UserContextService.NoActivePractitionerRoleException.class)
    public ResponseEntity<Map<String, Object>> handleNoActivePractitionerRole(
            UserContextService.NoActivePractitionerRoleException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(operationOutcome("forbidden", exception.getMessage()));
    }

    /**
     * Handles malformed JSON request bodies (e.g., parse errors).
     * Returns 400 with OperationOutcome instead of Spring's default 400 HTML.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(operationOutcome("invalid", "Corpo da requisição inválido ou malformado."));
    }

    /**
     * Handles Bean Validation failures (@Valid annotations).
     * Returns 400 with OperationOutcome listing all field errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String diagnostics = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        if (diagnostics.isBlank()) {
            diagnostics = "Dados de entrada inválidos.";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(operationOutcome("invalid", diagnostics));
    }

    private Map<String, Object> operationOutcome(String code, String diagnostics) {
        String safeDiagnostics = diagnostics == null || diagnostics.isBlank() ? "unexpected error" : diagnostics;
        return Map.of(
                "resourceType", "OperationOutcome",
                "issue", List.of(Map.of(
                        "severity", "error",
                        "code", code,
                        "details", Map.of("text", safeDiagnostics),
                        "diagnostics", safeDiagnostics
                ))
        );
    }

    private String tenantFailureReason(RuntimeException exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("path tenant does not match context tenant")) {
            return "path_mismatch";
        }
        if (exception instanceof TenantContextMissingException) {
            return "missing_header";
        }
        return "invalid_header";
    }

    private String currentTenantId() {
        String tenantId = MDC.get("tenant_id");
        return tenantId == null || tenantId.isBlank() ? "unknown" : tenantId;
    }
}
