package com.fap.auth.service;

import com.fap.auth.dto.AuthResponse;
import com.fap.auth.dto.ChangePasswordRequest;
import com.fap.auth.dto.ForgotPasswordRequest;
import com.fap.auth.dto.ResetPasswordRequest;
import com.fap.auth.entity.PasswordResetToken;
import com.fap.auth.entity.RefreshToken;
import com.fap.auth.repository.PasswordResetTokenRepository;
import com.fap.auth.repository.RefreshTokenRepository;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.UnauthorizedException;
import com.fap.common.metrics.DomainMetrics;
import com.fap.common.security.AuthorizationCache;
import com.fap.common.security.JwtService;
import com.fap.role.repository.RoleRepository;
import com.fap.user.entity.User;
import com.fap.user.mapper.UserMapper;
import com.fap.user.repository.UserRepository;
import com.fap.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-02T09:00:00Z"), ZoneOffset.UTC);
	private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

	private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
	private final JwtService jwtService = mock(JwtService.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
	private final PasswordResetTokenRepository passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
	private final PasswordResetMailService passwordResetMailService = mock(PasswordResetMailService.class);
	private final UserMapper userMapper = mock(UserMapper.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final DomainMetrics domainMetrics = mock(DomainMetrics.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final AuthorizationCache authorizationCache = mock(AuthorizationCache.class);
	// A real UserService: the password write is delegated to it, and the assertions below check
	// the user the auth flow loaded, not a stubbed call.
	private final UserService userService = new UserService(
			userRepository,
			mock(RoleRepository.class),
			passwordEncoder,
			userMapper,
			auditLogService,
			authorizationCache);
	private final AuthService authService = new AuthService(
			authenticationManager,
			jwtService,
			userRepository,
			userService,
			refreshTokenRepository,
			passwordResetTokenRepository,
			passwordResetMailService,
			userMapper,
			passwordEncoder,
			domainMetrics,
			CLOCK,
			7,
			15);

	@Test
	void changePasswordUpdatesHashAndRevokesRefreshTokens() {
		User user = new User();
		user.setId(1000L);
		user.setPasswordHash("old-hash");
		when(userRepository.findById(1000L)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("current-password", "old-hash")).thenReturn(true);
		when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

		authService.changePassword(1000L, new ChangePasswordRequest("current-password", "new-password"));

		assertThat(user.getPasswordHash()).isEqualTo("new-hash");
		assertThat(user.getUpdatedAt()).isNotNull();
		verify(refreshTokenRepository).revokeAllByUserId(1000L);
		verifyNoInteractions(auditLogService, authorizationCache);
	}

	@Test
	void changePasswordRejectsInvalidCurrentPassword() {
		User user = new User();
		user.setId(1000L);
		user.setPasswordHash("old-hash");
		when(userRepository.findById(1000L)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

		assertThatThrownBy(() -> authService.changePassword(
				1000L,
				new ChangePasswordRequest("wrong-password", "new-password")))
				.isInstanceOf(UnauthorizedException.class);

		verify(passwordEncoder, never()).encode("new-password");
		verify(refreshTokenRepository, never()).revokeAllByUserId(1000L);
	}

	@Test
	void forgotPasswordDoesNothingWhenEmailDoesNotExist() {
		when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

		authService.forgotPassword(new ForgotPasswordRequest("missing@example.com"));

		verify(passwordResetTokenRepository, never()).save(any());
	}

	@Test
	void resetPasswordUpdatesHashMarksTokenUsedAndRevokesRefreshTokens() {
		User user = new User();
		user.setId(1000L);
		user.setPasswordHash("old-hash");
		PasswordResetToken token = new PasswordResetToken();
		token.setUser(user);
		token.setExpiresAt(NOW.plusMinutes(10));
		when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(anyString()))
				.thenReturn(Optional.of(token));
		when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

		authService.resetPassword(new ResetPasswordRequest("123456", "new-password"));

		assertThat(user.getPasswordHash()).isEqualTo("new-hash");
		assertThat(token.isUsed()).isTrue();
		assertThat(token.getUsedAt()).isNotNull();
		verify(refreshTokenRepository).revokeAllByUserId(1000L);
		verifyNoInteractions(auditLogService, authorizationCache);
	}

	@Test
	void resetPasswordRejectsInvalidToken() {
		when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(anyString()))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("000000", "new-password")))
				.isInstanceOf(BadRequestException.class);

		verify(passwordEncoder, never()).encode("new-password");
	}

	@Test
	void changePasswordStampsUpdatedAtFromClock() {
		User user = new User();
		user.setId(1000L);
		user.setPasswordHash("old-hash");
		when(userRepository.findById(1000L)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("current-password", "old-hash")).thenReturn(true);
		when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

		authService.changePassword(1000L, new ChangePasswordRequest("current-password", "new-password"));

		assertThat(user.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	void forgotPasswordRetiresOldOtpsAndIssuesOneExpiringAfterTtl() {
		User user = new User();
		user.setId(1000L);
		when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

		authService.forgotPassword(new ForgotPasswordRequest(" User@Example.com "));

		verify(passwordResetTokenRepository).markUnusedByUserIdAsUsed(1000L, NOW);
		ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
		verify(passwordResetTokenRepository).save(saved.capture());
		assertThat(saved.getValue().getUser()).isSameAs(user);
		assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
		assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plusMinutes(15));
		verify(passwordResetMailService).sendPasswordResetOtp(eq(user), anyString(), eq(15L), any(Locale.class));
	}

	/**
	 * The mail is rendered after commit on another thread, where the request's locale context no
	 * longer exists, so the requester's language has to be captured here and handed over.
	 */
	@Test
	void forgotPasswordMailsTheOtpInTheRequestersLocale() {
		Locale vietnamese = Locale.forLanguageTag("vi");
		User user = new User();
		user.setId(1000L);
		when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
		LocaleContextHolder.setLocale(vietnamese);
		try {
			authService.forgotPassword(new ForgotPasswordRequest("user@example.com"));
		} finally {
			LocaleContextHolder.resetLocaleContext();
		}

		verify(passwordResetMailService).sendPasswordResetOtp(eq(user), anyString(), eq(15L), eq(vietnamese));
	}

	@Test
	void resetPasswordStampsUserAndTokenFromClock() {
		User user = new User();
		user.setId(1000L);
		PasswordResetToken token = new PasswordResetToken();
		token.setUser(user);
		token.setExpiresAt(NOW.plusSeconds(1));
		when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(anyString())).thenReturn(Optional.of(token));
		when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

		authService.resetPassword(new ResetPasswordRequest("123456", "new-password"));

		assertThat(user.getUpdatedAt()).isEqualTo(NOW);
		assertThat(token.getUsedAt()).isEqualTo(NOW);
	}

	@Test
	void resetPasswordRejectsTokenExpiringAtCurrentTime() {
		User user = new User();
		user.setId(1000L);
		PasswordResetToken token = new PasswordResetToken();
		token.setUser(user);
		token.setExpiresAt(NOW);
		when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(anyString())).thenReturn(Optional.of(token));

		assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("123456", "new-password")))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Invalid or expired OTP");

		assertThat(token.isUsed()).isFalse();
		verify(passwordEncoder, never()).encode("new-password");
		verify(refreshTokenRepository, never()).revokeAllByUserId(1000L);
	}

	@Test
	void refreshRevokesTokenAndIssuesOneExpiringAfterTtl() {
		User user = new User();
		user.setId(1000L);
		user.setEmail("user@example.com");
		RefreshToken existing = new RefreshToken();
		existing.setUser(user);
		existing.setToken("old-token");
		existing.setExpiresAt(NOW.plusSeconds(1));
		when(refreshTokenRepository.findByTokenAndRevokedFalse("old-token")).thenReturn(Optional.of(existing));
		when(jwtService.generateAccessToken(any())).thenReturn("access-token");

		AuthResponse response = authService.refresh("old-token");

		assertThat(existing.isRevoked()).isTrue();
		ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(saved.capture());
		assertThat(saved.getValue().getUser()).isSameAs(user);
		assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
		assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plusDays(7));
		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.refreshToken()).isEqualTo(saved.getValue().getToken());
	}

	@Test
	void refreshRejectsTokenExpiringAtCurrentTime() {
		RefreshToken existing = new RefreshToken();
		existing.setToken("old-token");
		existing.setExpiresAt(NOW);
		when(refreshTokenRepository.findByTokenAndRevokedFalse("old-token")).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> authService.refresh("old-token"))
				.isInstanceOf(UnauthorizedException.class)
				.hasMessage("Invalid refresh token");

		assertThat(existing.isRevoked()).isFalse();
		verify(refreshTokenRepository, never()).save(any());
	}
}
