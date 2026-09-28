package com.fap.common.security;

import com.fap.role.entity.Role;
import com.fap.role.repository.PermissionRepository;
import com.fap.user.entity.User;
import com.fap.user.enums.UserStatus;
import com.fap.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Every authenticated request resolves its principal through {@link FapUserDetailsService}, so
 * the roles and authorities built here are what {@code @PreAuthorize} and
 * {@link PermissionEvaluator} see. The token path is cached in {@link AuthorizationCache}: the tests
 * pin that the cache never holds a password hash, is keyed case-insensitively, does not remember
 * unknown users, and is only emptied once the writer's transaction commits.
 */
class FapUserDetailsServiceTest {

	private static final long USER_ID = 42L;
	private static final String EMAIL = "trainer@fap.local";
	private static final String PASSWORD_HASH = "$2a$10$hash";

	private final UserRepository userRepository = mock(UserRepository.class);
	private final AuthorizationCache authorizationCache =
			new AuthorizationCache(mock(PermissionRepository.class), Duration.ofMinutes(1));

	private final FapUserDetailsService service = new FapUserDetailsService(userRepository, authorizationCache);

	@AfterEach
	void clearTransactionSynchronization() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void loadUserBuildsRolesAndAuthoritiesFromEveryRole() {
		givenUser(UserStatus.Active, role(3L, RoleNames.TRAINER), role(4L, RoleNames.CLASS_ADMIN));

		FapUserPrincipal principal = service.loadUserByUsername(EMAIL);

		assertThat(principal.id()).isEqualTo(USER_ID);
		assertThat(principal.email()).isEqualTo(EMAIL);
		assertThat(principal.roles()).containsExactlyInAnyOrder(RoleNames.TRAINER, RoleNames.CLASS_ADMIN);
		assertThat(principal.getAuthorities())
				.extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder("ROLE_Trainer", "ROLE_ID_3", "ROLE_Class Admin", "ROLE_ID_4");
		assertThat(principal.isEnabled()).isTrue();
	}

	/** Password login verifies against this hash, so the uncached path must keep it. */
	@Test
	void loadUserKeepsPasswordHashForPasswordLogin() {
		givenUser(UserStatus.Active, role(3L, RoleNames.TRAINER));

		assertThat(service.loadUserByUsername(EMAIL).getPassword()).isEqualTo(PASSWORD_HASH);
	}

	@Test
	void loadUserDisablesInactiveUser() {
		givenUser(UserStatus.Inactive, role(3L, RoleNames.TRAINER));

		assertThat(service.loadUserByUsername(EMAIL).isEnabled()).isFalse();
	}

	@Test
	void loadUserWithoutRolesHasNoRolesOrAuthorities() {
		givenUser(UserStatus.Active);

		FapUserPrincipal principal = service.loadUserByUsername(EMAIL);

		assertThat(principal.roles()).isEmpty();
		assertThat(principal.getAuthorities()).isEmpty();
	}

	@Test
	void loadUserRejectsUnknownEmail() {
		when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.loadUserByUsername("nobody@fap.local"))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessage("User not found");
	}

	@Test
	void loadPrincipalForTokenStripsPasswordButKeepsIdentity() {
		givenUser(UserStatus.Active, role(3L, RoleNames.TRAINER));

		FapUserPrincipal principal = service.loadPrincipalForToken(EMAIL);

		assertThat(principal.getPassword()).isEmpty();
		assertThat(principal.id()).isEqualTo(USER_ID);
		assertThat(principal.roles()).containsExactly(RoleNames.TRAINER);
		assertThat(principal.getAuthorities())
				.extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder("ROLE_Trainer", "ROLE_ID_3");
		assertThat(principal.isEnabled()).isTrue();
	}

	/**
	 * The filter must still see a deactivated user from the cache, otherwise it could not reject the
	 * user's unexpired token.
	 */
	@Test
	void loadPrincipalForTokenKeepsDisabledFlag() {
		givenUser(UserStatus.Inactive, role(3L, RoleNames.TRAINER));

		assertThat(service.loadPrincipalForToken(EMAIL).isEnabled()).isFalse();
	}

	@Test
	void loadPrincipalForTokenServesRepeatedLookupsFromCache() {
		givenUser(UserStatus.Active, role(3L, RoleNames.TRAINER));

		FapUserPrincipal first = service.loadPrincipalForToken(EMAIL);
		FapUserPrincipal second = service.loadPrincipalForToken(EMAIL);

		assertThat(second).isSameAs(first);
		verify(userRepository, times(1)).findByEmailIgnoreCase(anyString());
	}

	/** Emails are case-insensitive logins, so differently cased tokens must share one entry. */
	@Test
	void loadPrincipalForTokenCachesCaseInsensitively() {
		givenUser(UserStatus.Active, role(3L, RoleNames.TRAINER));

		FapUserPrincipal upper = service.loadPrincipalForToken("Trainer@FAP.local");
		FapUserPrincipal lower = service.loadPrincipalForToken(EMAIL);

		assertThat(lower).isSameAs(upper);
		verify(userRepository, times(1)).findByEmailIgnoreCase(anyString());
		// The loader receives the normalized cache key, not the caller's spelling.
		verify(userRepository).findByEmailIgnoreCase(EMAIL);
	}

	/** Caching the failure would lock out a user created right after a failed lookup until the TTL. */
	@Test
	void loadPrincipalForTokenDoesNotCacheUnknownUser() {
		when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.loadPrincipalForToken("nobody@fap.local"))
				.isInstanceOf(UsernameNotFoundException.class);
		assertThatThrownBy(() -> service.loadPrincipalForToken("nobody@fap.local"))
				.isInstanceOf(UsernameNotFoundException.class);

		verify(userRepository, times(2)).findByEmailIgnoreCase("nobody@fap.local");
	}

	@Test
	void loadPrincipalForTokenCachesUserCreatedAfterFailedLookup() {
		when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.loadPrincipalForToken(EMAIL))
				.isInstanceOf(UsernameNotFoundException.class);
		givenUser(UserStatus.Active, role(3L, RoleNames.TRAINER));

		assertThat(service.loadPrincipalForToken(EMAIL).id()).isEqualTo(USER_ID);
	}

	/** Outside a transaction there is nothing to wait for, so the eviction happens immediately. */
	@Test
	void invalidatePrincipalsEvictsImmediatelyWithoutTransaction() {
		givenUser(UserStatus.Active, role(3L, RoleNames.TRAINER));
		service.loadPrincipalForToken(EMAIL);
		givenUser(UserStatus.Inactive, role(3L, RoleNames.TRAINER));

		authorizationCache.invalidatePrincipalsAfterCommit();

		assertThat(service.loadPrincipalForToken(EMAIL).isEnabled()).isFalse();
		verify(userRepository, times(2)).findByEmailIgnoreCase(EMAIL);
	}

	/**
	 * Evicting before commit would let a concurrent request re-cache the old row between the
	 * eviction and the commit, so inside a transaction the cached principal survives until commit.
	 */
	@Test
	void invalidatePrincipalsWaitsForTransactionCommit() {
		givenUser(UserStatus.Active, role(3L, RoleNames.TRAINER));
		service.loadPrincipalForToken(EMAIL);
		givenUser(UserStatus.Inactive, role(3L, RoleNames.TRAINER));
		TransactionSynchronizationManager.initSynchronization();

		authorizationCache.invalidatePrincipalsAfterCommit();

		assertThat(service.loadPrincipalForToken(EMAIL).isEnabled()).isTrue();
		verify(userRepository, times(1)).findByEmailIgnoreCase(EMAIL);

		TransactionSynchronizationUtils.triggerAfterCommit();

		assertThat(service.loadPrincipalForToken(EMAIL).isEnabled()).isFalse();
		verify(userRepository, times(2)).findByEmailIgnoreCase(EMAIL);
	}

	/** Permission edits must not force every user to be re-read. */
	@Test
	void invalidatingPermissionsKeepsCachedPrincipals() {
		givenUser(UserStatus.Active, role(3L, RoleNames.TRAINER));
		service.loadPrincipalForToken(EMAIL);

		authorizationCache.invalidatePermissionsAfterCommit();
		service.loadPrincipalForToken(EMAIL);

		verify(userRepository, times(1)).findByEmailIgnoreCase(EMAIL);
	}

	private void givenUser(UserStatus status, Role... roles) {
		User user = new User();
		user.setId(USER_ID);
		user.setEmail(EMAIL);
		user.setPasswordHash(PASSWORD_HASH);
		user.setStatus(status);
		user.setRoles(Set.of(roles));
		when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
	}

	private static Role role(long id, String name) {
		Role role = new Role();
		role.setId(id);
		role.setName(name);
		return role;
	}
}
