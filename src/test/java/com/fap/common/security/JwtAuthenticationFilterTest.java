package com.fap.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

	private static final String TOKEN = "signed.jwt.token";

	private final JwtService jwtService = mock(JwtService.class);
	private final FapUserDetailsService userDetailsService = mock(FapUserDetailsService.class);
	private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("authenticates an active user from the cached token principal")
	void authenticatesActiveUser() throws Exception {
		givenTokenFor(principal(true));

		filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(principal(true));
	}

	@Test
	@DisplayName("rejects the still-unexpired token of a deactivated user")
	void rejectsDeactivatedUser() throws Exception {
		givenTokenFor(principal(false));

		filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	private void givenTokenFor(FapUserPrincipal principal) {
		when(jwtService.extractSubject(TOKEN)).thenReturn(principal.email());
		when(userDetailsService.loadPrincipalForToken(principal.email())).thenReturn(principal);
	}

	private static MockHttpServletRequest requestWithToken() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me");
		request.addHeader("Authorization", "Bearer " + TOKEN);
		return request;
	}

	private static FapUserPrincipal principal(boolean enabled) {
		return new FapUserPrincipal(
				7L,
				"trainee@fap.local",
				"",
				Set.of("Trainee"),
				enabled,
				List.of(new SimpleGrantedAuthority("ROLE_ID_4")));
	}
}
