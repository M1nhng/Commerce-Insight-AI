package com.commerceinsight.user.mapper;

import com.commerceinsight.user.domain.User;
import com.commerceinsight.user.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.data.domain.Page;

/**
 * UserMapper — MapStruct mapper for the User module.
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>This is the ONLY place where User entity is converted to UserResponse in the user module.</li>
 *   <li>Never manually map entity fields in service code.</li>
 *   <li>componentModel = "spring" — Spring DI injects this as a bean.</li>
 * </ul>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    /**
     * Map a {@link User} entity to a {@link UserResponse} DTO.
     *
     * <p>fullName is derived from firstName + lastName via the entity's
     * {@code getFullName()} method. MapStruct calls the getter automatically.
     * passwordHash is intentionally NOT mapped.
     */
    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    UserResponse toUserResponse(User user);
}
