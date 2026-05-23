package com.fap.user.mapper;

import com.fap.role.mapper.RoleMapper;
import com.fap.user.dto.UserResponse;
import com.fap.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = RoleMapper.class)
public interface UserMapper {

	UserResponse toResponse(User user);
}
