package com.commerceinsight.export.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ExportProperties — configuration for the report export feature (Sprint 11A).
 *
 * <p>Bound from {@code app.export.*} in application.yml. Mirrors the pattern used by
 * {@code com.commerceinsight.dataimport.config.ImportProperties}.
 *
 * <p>Defaults (overridable via environment variables):
 * <ul>
 *   <li>maxRows = 10000 (env {@code EXPORT_MAX_ROWS})</li>
 * </ul>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.export")
public class ExportProperties {

    /**
     * Maximum number of data rows a single export may contain.
     * Requests whose result set would exceed this are rejected with a
     * business error instead of being silently truncated.
     */
    private int maxRows = 10000;
}
