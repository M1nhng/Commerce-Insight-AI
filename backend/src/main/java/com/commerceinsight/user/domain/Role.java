package com.commerceinsight.user.domain;

/**
 * Role — enumeration of user roles in Commerce Insight AI.
 *
 * <p>Roles are stored as VARCHAR(50) in the database.
 * Spring Security prefixes these with "ROLE_" for authority matching
 * (e.g., ADMIN → ROLE_ADMIN).
 *
 * <p>Role hierarchy (for documentation purposes — enforced via @PreAuthorize):
 * <ul>
 *   <li>ADMIN — Full system access including user management and audit logs</li>
 *   <li>MANAGER — Business operations (all modules except admin-only)</li>
 *   <li>STAFF — Day-to-day operations (orders, inventory; limited analytics)</li>
 * </ul>
 */
public enum Role {

    /**
     * Full administrative access: user management, system settings, audit logs.
     * Inherits all MANAGER and STAFF permissions.
     */
    ADMIN,

    /**
     * Business operations access: all modules except user management.
     * Can manage products, orders, customers, import/export.
     * Inherits all STAFF permissions.
     */
    MANAGER,

    /**
     * Day-to-day operations: order status updates, inventory view.
     * Limited analytics access. Cannot import, export, or manage business data.
     */
    STAFF
}
