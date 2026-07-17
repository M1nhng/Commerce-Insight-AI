package com.commerceinsight.user;

import com.commerceinsight.admin.service.AuditLogService;
import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.exception.DuplicateResourceException;
import com.commerceinsight.exception.ResourceNotFoundException;
import com.commerceinsight.user.domain.Role;
import com.commerceinsight.user.domain.User;
import com.commerceinsight.user.dto.request.ChangeRoleRequest;
import com.commerceinsight.user.dto.request.CreateUserRequest;
import com.commerceinsight.user.dto.request.UpdateUserRequest;
import com.commerceinsight.user.dto.response.UserResponse;
import com.commerceinsight.user.mapper.UserMapper;
import com.commerceinsight.user.repository.UserRepository;
import com.commerceinsight.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * UserServiceTest — unit tests for {@link UserService}.
 *
 * <p>All dependencies are mocked with Mockito.
 * No Spring context is loaded — this tests pure business logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private UserService userService;

    // Test fixtures
    private User testUser;
    private UserResponse testUserResponse;
    private final UUID testUserId  = UUID.randomUUID();
    private final UUID testAdminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash("$2a$12$hashed")
                .firstName("John")
                .lastName("Doe")
                .role(Role.STAFF)
                .active(true)
                .locked(false)
                .failedAttempts(0)
                .build();
        setFieldValue(testUser, "id", testUserId);

        testUserResponse = UserResponse.builder()
                .id(testUserId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .fullName("John Doe")
                .role(Role.STAFF)
                .active(true)
                .locked(false)
                .build();
    }

    // ── listUsers ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listUsers()")
    class ListUsersTests {

        @Test
        @DisplayName("should return paginated list of users")
        void listUsers_returnsPaginatedResult() {
            // Given
            PageRequest pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);
            given(userRepository.findAll(pageable)).willReturn(userPage);
            given(userMapper.toUserResponse(testUser)).willReturn(testUserResponse);

            // When
            Page<UserResponse> result = userService.listUsers(pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getEmail()).isEqualTo("test@example.com");
        }
    }

    // ── getUserById ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserById()")
    class GetUserByIdTests {

        @Test
        @DisplayName("should return user when found")
        void getUserById_found_returnsResponse() {
            given(userRepository.findById(testUserId)).willReturn(Optional.of(testUser));
            given(userMapper.toUserResponse(testUser)).willReturn(testUserResponse);

            UserResponse result = userService.getUserById(testUserId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testUserId);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void getUserById_notFound_throwsException() {
            given(userRepository.findById(any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── createUser ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("createUser()")
    class CreateUserTests {

        @Test
        @DisplayName("should create user and return UserResponse")
        void createUser_success() {
            // Given
            CreateUserRequest request = new CreateUserRequest(
                    "Alice", "Walker", "alice@example.com", "SecurePass@123", Role.MANAGER);

            given(userRepository.existsByEmail("alice@example.com")).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("$2a$12$hashed");
            given(userRepository.save(any(User.class))).willAnswer(inv -> {
                User u = inv.getArgument(0);
                setFieldValue(u, "id", UUID.randomUUID());
                return u;
            });
            given(userMapper.toUserResponse(any(User.class))).willReturn(testUserResponse);

            // When
            UserResponse result = userService.createUser(request, testAdminId);

            // Then
            assertThat(result).isNotNull();
            then(userRepository).should().save(any(User.class));
            then(auditLogService).should().log(eq(testAdminId), eq(AuditLogService.ACTION_USER_CREATED),
                    anyString(), any(), any(), anyString(), isNull());
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when email already exists")
        void createUser_duplicateEmail_throwsException() {
            // Given
            CreateUserRequest request = new CreateUserRequest(
                    "Bob", "Smith", "existing@example.com", "SecurePass@123", Role.STAFF);
            given(userRepository.existsByEmail("existing@example.com")).willReturn(true);

            // When / Then
            assertThatThrownBy(() -> userService.createUser(request, testAdminId))
                    .isInstanceOf(DuplicateResourceException.class);

            then(userRepository).should(never()).save(any());
        }
    }

    // ── changeRole ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("changeRole()")
    class ChangeRoleTests {

        @Test
        @DisplayName("should change role successfully")
        void changeRole_success() {
            // Given
            ChangeRoleRequest request = new ChangeRoleRequest(Role.MANAGER);
            given(userRepository.findById(testUserId)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);
            given(userMapper.toUserResponse(any(User.class))).willReturn(testUserResponse);

            // When
            userService.changeRole(testUserId, request, testAdminId);

            // Then
            assertThat(testUser.getRole()).isEqualTo(Role.MANAGER);
            then(auditLogService).should().log(eq(testAdminId), eq(AuditLogService.ACTION_USER_ROLE_CHANGED),
                    anyString(), any(), anyString(), anyString(), isNull());
        }

        @Test
        @DisplayName("should throw BusinessRuleException when admin tries to change own role")
        void changeRole_selfChange_throwsException() {
            // Given - admin tries to change their own role
            ChangeRoleRequest request = new ChangeRoleRequest(Role.STAFF);

            // When / Then
            assertThatThrownBy(() -> userService.changeRole(testAdminId, request, testAdminId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("own role");

            then(userRepository).should(never()).findById(any());
        }
    }

    // ── deleteUser ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUserTests {

        @Test
        @DisplayName("should soft-delete user by setting deletedAt")
        void deleteUser_success() {
            // Given
            given(userRepository.findById(testUserId)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            userService.deleteUser(testUserId, testAdminId);

            // Then
            assertThat(testUser.isDeleted()).isTrue();
            then(auditLogService).should().log(eq(testAdminId), eq(AuditLogService.ACTION_USER_DELETED),
                    anyString(), any(), anyString(), isNull(), isNull());
        }
    }

    // ── unlockUser ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("unlockUser()")
    class UnlockUserTests {

        @Test
        @DisplayName("should unlock account and reset failed attempts")
        void unlockUser_success() {
            // Given
            testUser.setLocked(true);
            testUser.setFailedAttempts(5);
            given(userRepository.findById(testUserId)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);
            given(userMapper.toUserResponse(any(User.class))).willReturn(testUserResponse);

            // When
            userService.unlockUser(testUserId, testAdminId);

            // Then
            assertThat(testUser.isLocked()).isFalse();
            assertThat(testUser.getFailedAttempts()).isEqualTo(0);
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    var field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new RuntimeException("Field not found: " + fieldName);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access field: " + fieldName, e);
        }
    }
}
