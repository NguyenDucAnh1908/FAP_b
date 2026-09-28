package com.fap.user.repository;

import com.fap.role.entity.Role;
import com.fap.role.repository.RoleRepository;
import com.fap.support.AbstractOracleIT;
import com.fap.user.dto.UserResponse;
import com.fap.user.entity.User;
import com.fap.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserSearchQueryIT extends AbstractOracleIT {

	private static final String MARKER = "it-user-search";
	private static final int FIXTURE_USERS = 25;
	private static final int PAGE_SIZE = 10;

	@Autowired
	private UserService userService;

	@Autowired
	private RoleRepository roleRepository;

	private Role trainer;

	@BeforeEach
	void createUsers() {
		Role trainee = roleRepository.findByNameIgnoreCase("Trainee").orElseThrow();
		trainer = roleRepository.findByNameIgnoreCase("Trainer").orElseThrow();
		LocalDateTime base = LocalDateTime.of(2030, 1, 1, 0, 0);
		for (int i = 0; i < FIXTURE_USERS; i++) {
			User user = new User();
			user.setFullName(MARKER + " user " + i);
			user.setEmail("%s-%02d@fap.local".formatted(MARKER, i));
			user.setPasswordHash("not-a-real-hash");
			user.setCreatedAt(base.plusMinutes(i));
			user.setUpdatedAt(base.plusMinutes(i));
			// Every user has two roles so a join-based query would produce duplicate rows.
			user.setRoles(new HashSet<>(Set.of(trainee, trainer)));
			entityManager.persist(user);
		}
		flushAndClear();
	}

	@Test
	void pagesAreCorrectAndDisjointWithMultiRoleUsers() {
		Page<UserResponse> first = search(null, 0);
		Page<UserResponse> second = search(null, 1);
		Page<UserResponse> third = search(null, 2);

		assertThat(first.getTotalElements()).isEqualTo(FIXTURE_USERS);
		assertThat(first.getTotalPages()).isEqualTo(3);
		assertThat(first.getContent()).hasSize(PAGE_SIZE);
		assertThat(third.getContent()).hasSize(FIXTURE_USERS - 2 * PAGE_SIZE);

		List<Long> ids = new java.util.ArrayList<>();
		first.forEach(user -> ids.add(user.id()));
		second.forEach(user -> ids.add(user.id()));
		third.forEach(user -> ids.add(user.id()));
		assertThat(ids).doesNotHaveDuplicates().hasSize(FIXTURE_USERS);
		// Default sort is createdAt desc: the newest fixture user comes first.
		assertThat(first.getContent().get(0).email()).isEqualTo(MARKER + "-24@fap.local");
		assertThat(first.getContent().get(0).roles()).hasSize(2);
	}

	@Test
	void roleFilterMatchesUsersHavingThatRoleAmongOthers() {
		Page<UserResponse> byId = userService.list(null, MARKER, null, null, trainer.getId(), null, 0, 50);
		Page<UserResponse> byName = userService.list(null, MARKER, null, null, null, "trainer", 0, 50);

		assertThat(byId.getTotalElements()).isEqualTo(FIXTURE_USERS);
		assertThat(byName.getTotalElements()).isEqualTo(FIXTURE_USERS);
	}

	/** Regression guard: the page must be cut in SQL, not after loading every matching user. */
	@Test
	void loadsOnlyTheRequestedPage() {
		Measured<Page<UserResponse>> measured = measure(() -> search(null, 0));

		assertThat(measured.result().getContent()).hasSize(PAGE_SIZE);
		// Page users + their roles (roles are shared, at most the 4 seeded roles).
		assertThat(measured.entitiesLoaded()).isLessThanOrEqualTo(PAGE_SIZE + 4);
		assertThat(measured.statements()).isLessThanOrEqualTo(4);
	}

	private Page<UserResponse> search(String roleName, int page) {
		return userService.list(null, MARKER, null, null, null, roleName, page, PAGE_SIZE);
	}
}
