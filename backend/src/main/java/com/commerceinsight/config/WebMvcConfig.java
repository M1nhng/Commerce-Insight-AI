package com.commerceinsight.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvcConfig — Spring MVC configuration.
 *
 * <p>Currently minimal — CORS is handled by {@link CorsConfig} via the
 * {@link org.springframework.web.cors.CorsConfigurationSource} bean,
 * which is picked up automatically by Spring Security's CORS support.
 *
 * <p>This class is the extension point for:
 * <ul>
 *   <li>Custom message converters (e.g., CSV, Excel response types for export)</li>
 *   <li>Custom argument resolvers (e.g., pagination parameter handling)</li>
 *   <li>Resource handlers (if serving static files)</li>
 *   <li>View controllers (if needed)</li>
 * </ul>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // CORS is configured via CorsConfig.corsConfigurationSource() bean.
    // Additional MVC configuration will be added as modules are implemented.
}
