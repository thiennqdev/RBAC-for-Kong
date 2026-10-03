package com.vcc.template.user.mapper;

import com.vcc.template.common.config.MapStructConfig;
import com.vcc.template.user.dto.UserResponse;
import com.vcc.template.user.entity.User;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for converting between User entity and DTOs.
 */
@Mapper(config = MapStructConfig.class)
public interface UserMapper {

    UserResponse toResponse(User user);
}
