package com.fap.user.repository;

import com.fap.user.entity.User;
import com.fap.user.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface UserRepository extends JpaRepository<User, Long> {

	@EntityGraph(attributePaths = "roles")
	Optional<User> findByEmailIgnoreCase(String email);

	@EntityGraph(attributePaths = "roles")
	@Query("select u from User u where u.id = :id")
	Optional<User> findWithRolesById(@Param("id") Long id);

	boolean existsByEmailIgnoreCase(String email);

	long countByStatus(UserStatus status);

	interface StatusCount {
		UserStatus getStatus();

		Long getTotal();
	}

	/** Row count per status in one query, for dashboards (replaces one count query per status). */
	@Query("select u.status as status, count(u) as total from User u group by u.status")
	List<StatusCount> countGroupedByStatus();

	interface RoleCount {
		String getRoleName();

		Long getTotal();
	}

	@Query("""
			select r.name as roleName, count(u) as total
			from User u
			join u.roles r
			where u.status = :status
			  and r.name in :roleNames
			group by r.name
			""")
	List<RoleCount> countByRoleNamesAndStatus(
			@Param("roleNames") Collection<String> roleNames,
			@Param("status") UserStatus status);

	/**
	 * Searches users and returns the requested page with roles initialized.
	 *
	 * <p>Two queries plus a count instead of one: fetching the {@code roles} collection in a paged
	 * query makes Hibernate load every matching user and cut the page in memory (HHH90003004). The
	 * page is cut in SQL over ids, then only those users are loaded with their roles.
	 */
	default Page<User> search(
			UserStatus status,
			String keyword,
			String email,
			String fullName,
			Long roleId,
			String roleName,
			Pageable pageable) {
		Page<Long> ids = searchIds(status, keyword, email, fullName, roleId, roleName, pageable);
		if (ids.isEmpty()) {
			return ids.map(id -> null);
		}
		Map<Long, User> usersById = findWithRolesByIdIn(ids.getContent()).stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));
		return ids.map(usersById::get);
	}

	// Role filters are EXISTS so a user with several roles is one row, without DISTINCT.
	@Query("""
			select u.id
			from User u
			where (:status is null or u.status = :status)
			  and (:keyword is null
			       or lower(u.email) like concat(concat('%', lower(:keyword)), '%')
			       or lower(u.fullName) like concat(concat('%', lower(:keyword)), '%')
			       or lower(coalesce(u.phone, '')) like concat(concat('%', lower(:keyword)), '%'))
			  and (:email is null or lower(u.email) like concat(concat('%', lower(:email)), '%'))
			  and (:fullName is null or lower(u.fullName) like concat(concat('%', lower(:fullName)), '%'))
			  and (:roleId is null or exists (
			      select r.id from u.roles r where r.id = :roleId))
			  and (:roleName is null or exists (
			      select r.id from u.roles r where lower(r.name) = lower(:roleName)))
			""")
	Page<Long> searchIds(
			@Param("status") UserStatus status,
			@Param("keyword") String keyword,
			@Param("email") String email,
			@Param("fullName") String fullName,
			@Param("roleId") Long roleId,
			@Param("roleName") String roleName,
			Pageable pageable);

	@EntityGraph(attributePaths = "roles")
	@Query("select u from User u where u.id in :ids")
	List<User> findWithRolesByIdIn(@Param("ids") Collection<Long> ids);

	@Query("""
			select count(u)
			from User u
			join u.roles r
			where r.name = :roleName
			  and u.status = :status
			""")
	long countByRoleNameAndStatus(@Param("roleName") String roleName, @Param("status") UserStatus status);
}
