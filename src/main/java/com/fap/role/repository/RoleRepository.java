package com.fap.role.repository;

import com.fap.common.exception.NotFoundException;
import com.fap.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoleRepository extends JpaRepository<Role, Long> {

	Optional<Role> findByNameIgnoreCase(String name);

	List<Role> findByNameIn(Collection<String> names);

	Set<Role> findByIdIn(Collection<Long> ids);

	/** One place for the lookup so every caller fails with the same not-found message. */
	default Role getRoleOrThrow(Long id) {
		return findById(id)
				.orElseThrow(() -> new NotFoundException("Role not found"));
	}
}
