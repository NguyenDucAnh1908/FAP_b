package com.fap.role.repository;

import com.fap.role.entity.Permission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

	@EntityGraph(attributePaths = "role")
	List<Permission> findByRoleIdIn(Collection<Long> roleIds);

	Optional<Permission> findByRoleIdAndResource(Long roleId, String resource);

	List<Permission> findByRoleId(Long roleId);
}
