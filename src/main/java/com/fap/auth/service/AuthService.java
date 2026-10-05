package com.fap.auth.service;

import com.fap.auth.dto.AuthResponse;
import com.fap.auth.dto.ChangePasswordRequest;
import com.fap.auth.dto.ForgotPasswordRequest;
import com.fap.auth.dto.LoginRequest;
import com.fap.auth.dto.ResetPasswordRequest;
import com.fap.auth.entity.PasswordResetToken;
import com.fap.auth.entity.RefreshToken;
import com.fap.auth.repository.PasswordResetTokenRepository;
import com.fap.auth.repository.RefreshTokenRepository;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.BusinessException;
import com.fap.common.exception.UnauthorizedException;
import com.fap.common.metrics.DomainMetrics;
import com.fap.common.security.FapUserPrincipal;
import com.fap.common.security.JwtService;
import com.fap.common.util.AfterCommit;
import com.fap.user.entity.User;
import com.fap.user.mapper.UserMapper;
import com.fap.user.repository.UserRepository;
import com.fap.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final UserRepository userRepository;
	private final UserService userService;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final PasswordResetMailService passwordResetMailService;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final DomainMetrics domainMetrics;
	private final Clock clock;
	private final long refreshTtlDays;
	private final long passwordResetTtlMinutes;
	private final SecureRandom secureRandom = new SecureRandom();

	public AuthService(
			AuthenticationManager authenticationManager,
			JwtService jwtService,
			UserRepository userRepository,
			UserService userService,
			RefreshTokenRepository refreshTokenRepository,
			PasswordResetTokenRepository passwordResetTokenRepository,
			PasswordResetMailService passwordResetMailService,
			UserMapper userMapper,
			PasswordEncoder passwordEncoder,
			DomainMetrics domainMetrics,
			Clock clock,
			@Value("${app.jwt.refresh-ttl-days:${JWT_REFRESH_TTL_DAYS:7}}") long refreshTtlDays,
			@Value("${app.password-reset.ttl-minutes:${PASSWORD_RESET_TTL_MINUTES:15}}") long passwordResetTtlMinutes) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.userRepository = userRepository;
		this.userService = userService;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.passwordResetMailService = passwordResetMailService;
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
		this.domainMetrics = domainMetrics;
		this.clock = clock;
		this.refreshTtlDays = refreshTtlDays;
		this.passwordResetTtlMinutes = passwordResetTtlMinutes;
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.email(), request.password()));
		} catch (AuthenticationException exception) {
			domainMetrics.recordLoginFailure();
			throw new UnauthorizedException("Invalid credentials");
		}
		FapUserPrincipal principal = (FapUserPrincipal) authentication.getPrincipal();
		User user = userRepository.findWithRolesById(principal.id())
				.orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
		return issueTokens(principal, user);
	}

	@Transactional
	public AuthResponse refresh(String token) {
		RefreshToken existing = refreshTokenRepository.findByTokenAndRevokedFalse(token)
				.filter(refreshToken -> refreshToken.getExpiresAt().isAfter(LocalDateTime.now(clock)))
				.orElseThrow(() -> new UnauthorizedException("Invalid refresh token")
						.withMessageKey("error.UNAUTHORIZED.refresh_token"));
		existing.setRevoked(true);
		User user = existing.getUser();
		FapUserPrincipal principal = new FapUserPrincipal(
				user.getId(),
				user.getEmail(),
				user.getPasswordHash(),
				user.getRoles().stream().map(role -> role.getName()).collect(java.util.stream.Collectors.toSet()),
				true,
				java.util.List.of());
		return issueTokens(principal, user);
	}

	@Transactional
	public void logout(String token) {
		refreshTokenRepository.findByTokenAndRevokedFalse(token)
				.ifPresent(refreshToken -> refreshToken.setRevoked(true));
	}

	@Transactional
	public void changePassword(Long userId, ChangePasswordRequest request) {
		// English stays the generic "Invalid credentials"; only the localized text is specific, which
		// is safe here because the caller is already authenticated (unlike login, see above).
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UnauthorizedException("Invalid credentials")
						.withMessageKey("error.UNAUTHORIZED.account"));
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new UnauthorizedException("Invalid credentials")
					.withMessageKey("error.UNAUTHORIZED.current_password");
		}
		userService.changePasswordHash(user, passwordEncoder.encode(request.newPassword()), LocalDateTime.now(clock));
		refreshTokenRepository.revokeAllByUserId(userId);
	}

	@Transactional
	public void forgotPassword(ForgotPasswordRequest request) {
		// Captured on the request thread: the mail is rendered after commit on an async thread, where
		// the request's Accept-Language is no longer in the LocaleContextHolder.
		Locale locale = LocaleContextHolder.getLocale();
		userRepository.findByEmailIgnoreCase(request.email().trim().toLowerCase())
				.ifPresent(user -> {
					LocalDateTime now = LocalDateTime.now(clock);
					passwordResetTokenRepository.markUnusedByUserIdAsUsed(user.getId(), now);
					String otp = newOtp();
					PasswordResetToken token = new PasswordResetToken();
					token.setUser(user);
					token.setTokenHash(sha256(otp));
					token.setExpiresAt(now.plusMinutes(passwordResetTtlMinutes));
					token.setCreatedAt(now);
					passwordResetTokenRepository.save(token);
					// After commit, so the OTP is only mailed once it is redeemable; asynchronously, so
					// the SMTP round trip neither holds the transaction open nor makes a known email
					// measurably slower to answer than an unknown one.
					AfterCommit.run(() -> passwordResetMailService.sendPasswordResetOtp(user, otp, passwordResetTtlMinutes, locale));
				});
	}

	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		LocalDateTime now = LocalDateTime.now(clock);
		PasswordResetToken token = passwordResetTokenRepository.findByTokenHashAndUsedFalse(sha256(request.otp()))
				.filter(existing -> existing.getExpiresAt().isAfter(now))
				.orElseThrow(() -> new BadRequestException("INVALID_RESET_TOKEN", "Invalid or expired OTP"));
		User user = token.getUser();
		userService.changePasswordHash(user, passwordEncoder.encode(request.newPassword()), now);
		token.setUsed(true);
		token.setUsedAt(now);
		refreshTokenRepository.revokeAllByUserId(user.getId());
	}

	public AuthResponse googleLogin(String idToken) {
		throw new BusinessException("GOOGLE_AUTH_NOT_CONFIGURED", "Google login provider is not configured");
	}

	private AuthResponse issueTokens(FapUserPrincipal principal, User user) {
		String accessToken = jwtService.generateAccessToken(principal);
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setUser(user);
		refreshToken.setToken(newRefreshToken());
		LocalDateTime now = LocalDateTime.now(clock);
		refreshToken.setExpiresAt(now.plusDays(refreshTtlDays));
		refreshToken.setCreatedAt(now);
		refreshTokenRepository.save(refreshToken);
		return new AuthResponse(
				accessToken,
				refreshToken.getToken(),
				"Bearer",
				jwtService.accessTtlSeconds(),
				userMapper.toResponse(user));
	}

	private String newRefreshToken() {
		return newSecureToken();
	}

	private String newSecureToken() {
		byte[] bytes = new byte[48];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String newOtp() {
		return "%06d".formatted(secureRandom.nextInt(1_000_000));
	}

	private String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}
}
