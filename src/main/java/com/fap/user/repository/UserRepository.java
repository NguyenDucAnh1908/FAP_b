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

	long countByStatus(UserStatus status);

	@EntityGraph(attributePaths = "roles")
	@Query("""
			select distinct u
			from User u
			left join u.roles r
			where (:status is null or u.status = :status)
			  and (:keyword is null
			       or lower(u.email) like concat(concat('%', lower(:keyword)), '%')
			       or lower(u.fullName) like concat(concat('%', lower(:keyword)), '%')
			       or lower(coalesce(u.phone, '')) like concat(concat('%', lower(:keyword)), '%'))
			  and (:email is null or lower(u.email) like concat(concat('%', lower(:email)), '%'))
			  and (:fullName is null or lower(u.fullName) like concat(concat('%', lower(:fullName)), '%'))
			  and (:roleId is null or r.id = :roleId)
			  and (:roleName is null or lower(r.name) = lower(:roleName))
			""")
	Page<User> search(
			@Param("status") UserStatus status,
			@Param("keyword") String keyword,
			@Param("email") String email,
			@Param("fullName") String fullName,
			@Param("roleId") Long roleId,
			@Param("roleName") String roleName,
			Pageable pageable);

	@Query("""
			select count(u)
			from User u
			join u.roles r
			where r.name = :roleName
			  and u.status = :status
			""")
	long countByRoleNameAndStatus(@Param("roleName") String roleName, @Param("status") UserStatus status);
}
