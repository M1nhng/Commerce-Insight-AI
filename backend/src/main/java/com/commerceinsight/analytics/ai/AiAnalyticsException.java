package com.commerceinsight.analytics.ai;

import com.commerceinsight.shared.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * AiAnalyticsException — request-level problems with an AI-insights call
 * (invalid / oversized date range). Carries the {@link ErrorCode} and the
 * {@link HttpStatus} the handler should return, exactly like
 * {@code com.commerceinsight.export.exception.ExportException}. Mapped in
 * {@code GlobalExceptionHandler}.
 *
 * <p>Provider failures do NOT use this type — they are swallowed inside
 * {@link AiAnalyticsService} and returned as an {@code available:false}
 * response with HTTP 200.
 */
@Getter
public class AiAnalyticsException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public AiAnalyticsException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    /** HTTP 400 — {@code dateFrom} is not before {@code dateTo}. */
    public static AiAnalyticsException invalidRange() {
        return new AiAnalyticsException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                "dateFrom must be before dateTo");
    }

    /** HTTP 400 — the window is longer than the configured maximum. */
    public static AiAnalyticsException rangeTooLarge(int maxDays) {
        return new AiAnalyticsException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                "The analysis window must not exceed " + maxDays + " days");
    }
}
