package com.fap.auth.service;

import com.fap.auth.dto.AuthResponse;
import com.fap.auth.dto.LoginRequest;
import com.fap.auth.entity.RefreshToken;
import com.fap.auth.repository.RefreshTokenRepository;
import com.fap.common.exception.BusinessException;
import com.fap.common.exception.UnauthorizedException;
import com.fap.common.security.FapUserPrincipal;
import com.fap.common.security.JwtService;
import com.fap.user.entity.User;
import com.fap.user.mapper.UserMapper;
import com.fap.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserMapper userMapper;
	private final long refreshTtlDays;
	private final SecureRandom secureRandom = new SecureRandom();

	public AuthService(
			AuthenticationManager authenticationManager,
			JwtService jwtService,
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			UserMapper userMapper,
			@Value("${app.jwt.refresh-ttl-days:${JWT_REFRESH_TTL_DAYS:7}}") long refreshTtlDays) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.userMapper = userMapper;
		this.refreshTtlDays = refreshTtlDays;
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.email(), request.password()));
		FapUserPrincipal principal = (FapUserPrincipal) authentication.getPrincipal();
		User user = userRepository.findWithRolesById(principal.id())
				.orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
		return issueTokens(principal, user);
	}

	@Transactional
	public AuthResponse refresh(String token) {
		RefreshToken existing = refreshTokenRepository.findByTokenAndRevokedFalse(token)
				.filter(refreshToken -> refreshToken.getExpiresAt().isAfter(LocalDateTime.now()))
				.orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
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

	public AuthResponse googleLogin(String idToken) {
		throw new BusinessException("GOOGLE_AUTH_NOT_CONFIGURED", "Google login provider is not configured");
	}

	private AuthResponse issueTokens(FapUserPrincipal principal, User user) {
		String accessToken = jwtService.generateAccessToken(principal);
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setUser(user);
		refreshToken.setToken(newRefreshToken());
		refreshToken.setExpiresAt(LocalDateTime.now().plusDays(refreshTtlDays));
		refreshToken.setCreatedAt(LocalDateTime.now());
		refreshTokenRepository.save(refreshToken);
		return new AuthResponse(
				accessToken,
				refreshToken.getToken(),
				"Bearer",
				jwtService.accessTtlSeconds(),
				userMapper.toResponse(user));
	}

	private String newRefreshToken() {
		byte[] bytes = new byte[48];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
