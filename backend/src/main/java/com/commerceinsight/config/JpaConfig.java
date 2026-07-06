package com.commerceinsight.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Optional;

/**
 * JpaConfig — JPA / Hibernate / Spring Data configuration.
 *
 * <p>Enables:
 * <ul>
 *   <li>JPA Auditing — auto-populates createdAt and updatedAt on BaseEntity</li>
 *   <li>Transaction management</li>
 *   <li>JPA repositories scanning</li>
 * </ul>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableJpaRepositories(basePackages = "com.commerceinsight")
@EnableTransactionManagement
public class JpaConfig {

    /**
     * AuditorAware — provides the current auditor (user ID) for JPA Auditing.
     *
     * <p>This is used to auto-populate @CreatedBy and @LastModifiedBy fields
     * if they are added to entities in the future.
     *
     * <p>Returns the current authenticated user's UUID string,
     * or "system" for background/unauthenticated operations.
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of("system");
            }
            return Optional.of(authentication.getName());
        };
    }
}
