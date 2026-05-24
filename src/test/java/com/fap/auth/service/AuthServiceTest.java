package com.fap.auth.service;

import com.fap.auth.dto.ChangePasswordRequest;
import com.fap.auth.dto.ForgotPasswordRequest;
import com.fap.auth.dto.ResetPasswordRequest;
import com.fap.auth.entity.PasswordResetToken;
import com.fap.auth.repository.PasswordResetTokenRepository;
import com.fap.auth.repository.RefreshTokenRepository;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.UnauthorizedException;
import com.fap.common.security.JwtService;
import com.fap.user.entity.User;
import com.fap.user.mapper.UserMapper;
import com.fap.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

	private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
	private final JwtService jwtService = mock(JwtService.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
	private final PasswordResetTokenRepository passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
	private final PasswordResetMailService passwordResetMailService = mock(PasswordResetMailService.class);
	private final UserMapper userMapper = mock(UserMapper.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final AuthService authService = new AuthService(
			authenticationManager,
			jwtService,
			userRepository,
			refreshTokenRepository,
			passwordResetTokenRepository,
			passwordResetMailService,
			userMapper,
			passwordEncoder,
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

		verify(passwordResetTokenRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void resetPasswordUpdatesHashMarksTokenUsedAndRevokesRefreshTokens() {
		User user = new User();
		user.setId(1000L);
		user.setPasswordHash("old-hash");
		PasswordResetToken token = new PasswordResetToken();
		token.setUser(user);
		token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
		when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(org.mockito.ArgumentMatchers.anyString()))
				.thenReturn(Optional.of(token));
		when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

		authService.resetPassword(new ResetPasswordRequest("123456", "new-password"));

		assertThat(user.getPasswordHash()).isEqualTo("new-hash");
		assertThat(token.isUsed()).isTrue();
		assertThat(token.getUsedAt()).isNotNull();
		verify(refreshTokenRepository).revokeAllByUserId(1000L);
	}

	@Test
	void resetPasswordRejectsInvalidToken() {
		when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(org.mockito.ArgumentMatchers.anyString()))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("000000", "new-password")))
				.isInstanceOf(BadRequestException.class);

		verify(passwordEncoder, never()).encode("new-password");
	}
}
