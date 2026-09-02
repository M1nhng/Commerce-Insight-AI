package com.commerceinsight.exception;

import com.commerceinsight.analytics.ai.AiAnalyticsException;
import com.commerceinsight.export.exception.ExportException;
import com.commerceinsight.shared.dto.ApiResponse;
import com.commerceinsight.shared.dto.ErrorResponse;
import com.commerceinsight.shared.exception.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler — centralized exception-to-HTTP-response mapping.
 *
 * <p>Architecture Rule: ALL exceptions must be caught here.
 * Controllers must NEVER catch exceptions themselves — they must propagate.
 * Business services throw domain exceptions; this handler maps them to responses.
 *
 * <p>No internal stack traces or server details are ever exposed to clients.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Domain Exceptions ────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.debug("Resource not found at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorResponse.of(ex.getErrorCode().name(), ex.getMessage())));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRule(
            BusinessRuleException ex, HttpServletRequest request) {
        log.warn("Business rule violation at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ErrorResponse.of(ex.getErrorCode().name(), ex.getMessage())));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {
        log.debug("Duplicate resource at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ErrorResponse.of(ex.getErrorCode().name(), ex.getMessage())));
    }

    @ExceptionHandler(ImportException.class)
    public ResponseEntity<ApiResponse<Void>> handleImport(
            ImportException ex, HttpServletRequest request) {
        log.warn("Import error at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ErrorResponse.of(
                        ErrorCode.IMPORT_VALIDATION_FAILED.name(), ex.getMessage())));
    }

    /**
     * Export module errors (Sprint 11A). The exception carries both the
     * {@link ErrorCode} and the {@link HttpStatus} to return
     * (400 bad format / bad date range, 422 row-limit exceeded, 500 generation
     * failure). Messages are pre-sanitised; no cause detail is exposed.
     */
    @ExceptionHandler(ExportException.class)
    public ResponseEntity<ApiResponse<Void>> handleExport(
            ExportException ex, HttpServletRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("Export generation failed at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        } else {
            log.warn("Export request rejected at {}: {}", request.getRequestURI(), ex.getMessage());
        }
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ErrorResponse.of(ex.getErrorCode().name(), ex.getMessage())));
    }

    /**
     * AI-insights request-level problems (invalid / oversized date range).
     * Carries its own {@link ErrorCode} + {@link HttpStatus}, same style as
     * {@link ExportException}. Provider failures never reach here — they are
     * handled inside the service and returned as an {@code available:false} 200.
     */
    @ExceptionHandler(AiAnalyticsException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiAnalytics(
            AiAnalyticsException ex, HttpServletRequest request) {
        log.debug("AI analytics request rejected at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ErrorResponse.of(ex.getErrorCode().name(), ex.getMessage())));
    }

    // ── Validation ───────────────────────────────────────────────────────

    /**
     * Handles @Valid / @Validated failures on @RequestBody.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ErrorResponse.FieldError.of(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());
        log.debug("Validation failed: {} field errors", details.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorResponse.withDetails(
                        ErrorCode.VALIDATION_ERROR.name(),
                        "Request validation failed",
                        details)));
    }

    /**
     * Handles @Valid on @ModelAttribute or form data.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException ex) {
        List<ErrorResponse.FieldError> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ErrorResponse.FieldError.of(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorResponse.withDetails(
                        ErrorCode.VALIDATION_ERROR.name(), "Binding validation failed", details)));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(
            MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorResponse.of(
                        ErrorCode.VALIDATION_ERROR.name(),
                        "Required parameter '" + ex.getParameterName() + "' is missing")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorResponse.of(
                        ErrorCode.VALIDATION_ERROR.name(),
                        "Invalid value for parameter '" + ex.getName() + "'")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorResponse.of(
                        ErrorCode.VALIDATION_ERROR.name(), "Request body is malformed or missing")));
    }

    /**
     * {@code @Validated} failures on path variables / request params (and any
     * other bean-validation constraint violation outside a request body).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorResponse.FieldError> details = ex.getConstraintViolations().stream()
                .map(v -> ErrorResponse.FieldError.of(
                        v.getPropertyPath() == null ? null : v.getPropertyPath().toString(),
                        v.getMessage()))
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorResponse.withDetails(
                        ErrorCode.VALIDATION_ERROR.name(), "Request validation failed", details)));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingPart(MissingServletRequestPartException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorResponse.of(
                        ErrorCode.VALIDATION_ERROR.name(),
                        "Required file part '" + ex.getRequestPartName() + "' is missing")));
    }

    /**
     * DB constraint hit that was not caught by a domain rule first. Never
     * surfaces the underlying SQL / constraint name.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation at {}: {}", request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ErrorResponse.of(
                        ErrorCode.RESOURCE_CONFLICT.name(),
                        "The request conflicts with the current state of the resource")));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(ErrorResponse.of(
                        ErrorCode.PAYLOAD_TOO_LARGE.name(),
                        "The uploaded file exceeds the maximum allowed size")));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(ErrorResponse.of(
                        ErrorCode.UNSUPPORTED_MEDIA_TYPE.name(),
                        "The request media type is not supported by this endpoint")));
    }

    // ── Security ─────────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorResponse.of(
                        ErrorCode.ACCESS_DENIED.name(), "You do not have permission to perform this action")));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorResponse.of(
                        ErrorCode.INVALID_CREDENTIALS.name(), "Invalid email or password")));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorResponse.of(
                        ErrorCode.ACCOUNT_DISABLED.name(), "Your account has been deactivated")));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(LockedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorResponse.of(
                        ErrorCode.ACCOUNT_LOCKED.name(), "Your account is locked. Please contact an administrator")));
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpiredJwt(ExpiredJwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorResponse.of(
                        ErrorCode.TOKEN_EXPIRED.name(), "Access token has expired")));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwtException(JwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorResponse.of(
                        ErrorCode.TOKEN_INVALID.name(), "Access token is invalid")));
    }

    // ── HTTP Protocol ─────────────────────────────────────────────────────

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(ErrorResponse.of(
                        "METHOD_NOT_ALLOWED", ex.getMessage())));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorResponse.of(
                        ErrorCode.RESOURCE_NOT_FOUND.name(), "The requested endpoint does not exist")));
    }

    /**
     * No controller mapped the request path (thrown because
     * {@code spring.web.resources.add-mappings=false}). An unknown path is a 404,
     * not a 500 — and the message never echoes the path.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(NoHandlerFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorResponse.of(
                        ErrorCode.RESOURCE_NOT_FOUND.name(), "The requested endpoint does not exist")));
    }

    // ── Catch-All ────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorResponse.of(
                        ErrorCode.INTERNAL_ERROR.name(),
                        "An unexpected error occurred. Please try again later")));
    }
}
