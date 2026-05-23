package com.fap.user.repository;

import com.fap.user.entity.User;
import com.fap.user.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	@EntityGraph(attributePaths = "roles")
	Optional<User> findByEmailIgnoreCase(String email);

	@EntityGraph(attributePaths = "roles")
	@Query("select u from User u where u.id = :id")
	Optional<User> findWithRolesById(@Param("id") Long id);

	boolean existsByEmailIgnoreCase(String email);

	Page<User> findByStatus(UserStatus status, Pageable pageable);
}
