package com.commerceinsight.auth.mapper;

import com.commerceinsight.auth.dto.response.UserResponse;
import com.commerceinsight.user.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * AuthMapper — MapStruct mapper for Auth module entity ↔ DTO conversions.
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>This is the ONLY place where User entity is converted to UserResponse.</li>
 *   <li>Never manually map fields in service code — always use this mapper.</li>
 *   <li>componentModel = "spring" — Spring DI injects this as a bean.</li>
 * </ul>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthMapper {

    /**
     * Map a {@link User} entity to a {@link UserResponse} DTO.
     *
     * <p>fullName is derived from firstName + lastName via the entity's
     * {@code getFullName()} method. MapStruct will call the getter automatically.
     *
     * <p>passwordHash is intentionally NOT mapped (no corresponding field in UserResponse).
     */
    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    UserResponse toUserResponse(User user);
}
