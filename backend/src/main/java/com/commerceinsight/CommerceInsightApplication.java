package com.commerceinsight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Commerce Insight AI — Spring Boot Application Entry Point.
 *
 * <p>This is a Modular Monolith. Domain modules are organized as sub-packages
 * under {@code com.commerceinsight}. Each module owns its own controller,
 * service, repository, domain, dto, and mapper layers.
 *
 * <p>Architecture rules:
 * <ul>
 *   <li>No entity exposure to the frontend — DTOs only.</li>
 *   <li>No business logic in controllers — service layer only.</li>
 *   <li>No cross-module repository access.</li>
 *   <li>All entity-DTO conversions via MapStruct mappers.</li>
 * </ul>
 *
 * <p>{@code @EnableScheduling} activates the {@code AccountUnlockScheduler}
 * which auto-unlocks accounts after 15 minutes of lock time.
 *
 * <p>{@code @EnableAsync} activates {@code @Async} audit log writes so that
 * audit events never block the main request thread.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class CommerceInsightApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommerceInsightApplication.class, args);
    }
}
