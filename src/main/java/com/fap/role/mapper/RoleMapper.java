package com.fap.role.mapper;

import com.fap.role.dto.PermissionResponse;
import com.fap.role.dto.RoleResponse;
import com.fap.role.entity.Permission;
import com.fap.role.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {

	RoleResponse toResponse(Role role);

	@Mapping(target = "roleId", source = "role.id")
	@Mapping(target = "roleName", source = "role.name")
	PermissionResponse toResponse(Permission permission);
}
