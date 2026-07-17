package com.commerceinsight.user.service;

import com.commerceinsight.admin.service.AuditLogService;
import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.exception.DuplicateResourceException;
import com.commerceinsight.exception.ResourceNotFoundException;
import com.commerceinsight.shared.dto.PageResponse;
import com.commerceinsight.shared.exception.ErrorCode;
import com.commerceinsight.user.domain.Role;
import com.commerceinsight.user.domain.User;
import com.commerceinsight.user.dto.request.ChangeRoleRequest;
import com.commerceinsight.user.dto.request.CreateUserRequest;
import com.commerceinsight.user.dto.request.UpdateUserRequest;
import com.commerceinsight.user.dto.response.UserResponse;
import com.commerceinsight.user.mapper.UserMapper;
import com.commerceinsight.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * UserService — business logic for user management (ADMIN operations).
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>List, fetch, create, update, and soft-delete users</li>
 *   <li>Change user roles</li>
 *   <li>Unlock locked accounts</li>
 * </ul>
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>All methods return DTOs — never return the User entity.</li>
 *   <li>All mapping via {@link UserMapper}.</li>
 *   <li>All significant state changes emit an audit log entry.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    // ── List ─────────────────────────────────────────────────────────────

    /**
     * Return a paginated list of all non-deleted users.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of UserResponse DTOs
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toUserResponse);
    }

    // ── Get By ID ─────────────────────────────────────────────────────────

    /**
     * Fetch a single user by their UUID.
     *
     * @param userId the UUID to look up
     * @return the user's profile as a UserResponse
     * @throws ResourceNotFoundException if no active user exists with this ID
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = findUserOrThrow(userId);
        return userMapper.toUserResponse(user);
    }

    // ── Create ───────────────────────────────────────────────────────────

    /**
     * Create a new user account with the specified role (ADMIN operation).
     *
     * @param request the creation request
     * @param adminId the ID of the admin performing this action (for auditing)
     * @return the newly created user's profile
     * @throws DuplicateResourceException if the email is already registered
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request, UUID adminId) {
        String email = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw DuplicateResourceException.email(email);
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .role(request.role())
                .active(true)
                .locked(false)
                .failedAttempts(0)
                .build();

        User saved = userRepository.save(user);
        log.info("User created by admin {}: {} ({})", adminId, saved.getEmail(), saved.getId());

        auditLogService.log(adminId, AuditLogService.ACTION_USER_CREATED, "User", saved.getId(),
                null, String.format("{\"email\":\"%s\",\"role\":\"%s\"}", saved.getEmail(), saved.getRole()), null);

        return userMapper.toUserResponse(saved);
    }

    // ── Update ───────────────────────────────────────────────────────────

    /**
     * Update a user's profile fields (firstName, lastName, email, active status).
     *
     * @param userId  the ID of the user to update
     * @param request the update request
     * @param adminId the ID of the admin performing this action
     * @return the updated user profile
     * @throws ResourceNotFoundException  if the user does not exist
     * @throws DuplicateResourceException if the new email is taken by another user
     */
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request, UUID adminId) {
        User user = findUserOrThrow(userId);
        String oldEmail = user.getEmail();

        String newEmail = request.email().toLowerCase().trim();
        if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw DuplicateResourceException.email(newEmail);
        }

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(newEmail);
        user.setActive(request.active());

        User updated = userRepository.save(user);
        log.info("User {} updated by admin {}", userId, adminId);

        auditLogService.log(adminId, AuditLogService.ACTION_USER_UPDATED, "User", userId,
                String.format("{\"email\":\"%s\"}", oldEmail),
                String.format("{\"email\":\"%s\",\"active\":%b}", newEmail, request.active()), null);

        return userMapper.toUserResponse(updated);
    }

    // ── Change Role ──────────────────────────────────────────────────────

    /**
     * Change a user's role.
     *
     * <p>Security Rule: An admin cannot change their own role.
     *
     * @param targetUserId the user whose role is being changed
     * @param request      the new role
     * @param adminId      the ID of the admin performing this action
     * @throws BusinessRuleException if the admin tries to change their own role
     */
    @Transactional
    public UserResponse changeRole(UUID targetUserId, ChangeRoleRequest request, UUID adminId) {
        if (targetUserId.equals(adminId)) {
            throw new BusinessRuleException(ErrorCode.USER_CANNOT_CHANGE_OWN_ROLE,
                    "You cannot change your own role");
        }

        User user = findUserOrThrow(targetUserId);
        Role oldRole = user.getRole();
        user.setRole(request.role());
        User updated = userRepository.save(user);

        log.info("User {} role changed from {} to {} by admin {}", targetUserId, oldRole, request.role(), adminId);

        auditLogService.log(adminId, AuditLogService.ACTION_USER_ROLE_CHANGED, "User", targetUserId,
                String.format("{\"role\":\"%s\"}", oldRole),
                String.format("{\"role\":\"%s\"}", request.role()), null);

        return userMapper.toUserResponse(updated);
    }

    // ── Soft Delete ──────────────────────────────────────────────────────

    /**
     * Soft-delete a user account by setting deletedAt.
     *
     * @param userId  the user to delete
     * @param adminId the admin performing the deletion
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional
    public void deleteUser(UUID userId, UUID adminId) {
        User user = findUserOrThrow(userId);
        user.softDelete();
        userRepository.save(user);

        log.info("User {} soft-deleted by admin {}", userId, adminId);

        auditLogService.log(adminId, AuditLogService.ACTION_USER_DELETED, "User", userId,
                String.format("{\"email\":\"%s\"}", user.getEmail()), null, null);
    }

    // ── Unlock Account ───────────────────────────────────────────────────

    /**
     * Manually unlock a locked user account.
     *
     * @param userId  the user to unlock
     * @param adminId the admin performing the unlock
     */
    @Transactional
    public UserResponse unlockUser(UUID userId, UUID adminId) {
        User user = findUserOrThrow(userId);
        user.setLocked(false);
        user.resetFailedAttempts();
        User updated = userRepository.save(user);

        log.info("User {} manually unlocked by admin {}", userId, adminId);

        auditLogService.log(adminId, AuditLogService.ACTION_USER_UNLOCKED, "User", userId,
                null, null, null);

        return userMapper.toUserResponse(updated);
    }

    // ── Internal Helpers ─────────────────────────────────────────────────

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                        "User with ID '" + userId + "' was not found"));
    }
}
