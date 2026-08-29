package com.commerceinsight.dataimport.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ImportProperties — configuration for the data import feature.
 *
 * <p>Bound from {@code app.import.*} in application.yml.
 *
 * <p>Defaults (can be overridden via environment variables):
 * <ul>
 *   <li>maxFileSizeMb = 10</li>
 *   <li>maxRows = 5000</li>
 * </ul>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.import")
public class ImportProperties {

    /**
     * Maximum allowed file size in megabytes.
     * Files exceeding this size are rejected before parsing.
     */
    private int maxFileSizeMb = 10;

    /**
     * Maximum number of data rows allowed per import file.
     * Files with more rows are rejected to prevent memory exhaustion.
     */
    private int maxRows = 5000;

    /** Returns maxFileSizeMb converted to bytes. */
    public long maxFileSizeBytes() {
        return (long) maxFileSizeMb * 1024 * 1024;
    }
}
