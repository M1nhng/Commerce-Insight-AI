package com.commerceinsight.auth.repository;

import com.commerceinsight.auth.domain.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * LoginHistoryRepository — write path for {@link LoginHistory}.
 *
 * <p>Architecture rule: only {@link com.commerceinsight.auth.service.LoginHistoryService}
 * writes here. There is no HTTP read surface for login history in Sprint 12A;
 * it is queried directly against the database for security investigation.
 */
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, UUID> {
}
