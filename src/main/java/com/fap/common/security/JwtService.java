package com.fap.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

	private final SecretKey signingKey;
	private final long accessTtlSeconds;

	public JwtService(
			@Value("${app.jwt.secret:${JWT_SECRET:change_me_dev_secret}}") String secret,
			@Value("${app.jwt.access-ttl-minutes:${JWT_ACCESS_TTL_MINUTES:15}}") long accessTtlMinutes) {
		this.signingKey = Keys.hmacShaKeyFor(sha256(secret));
		this.accessTtlSeconds = accessTtlMinutes * 60;
	}

	public String generateAccessToken(FapUserPrincipal principal) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(principal.getUsername())
				.claims(Map.of("userId", principal.id(), "roles", principal.roles()))
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
				.signWith(signingKey)
				.compact();
	}

	public String extractSubject(String token) {
		return claims(token).getSubject();
	}

	public boolean isValid(String token, UserDetails userDetails) {
		Claims claims = claims(token);
		return claims.getSubject().equals(userDetails.getUsername())
				&& claims.getExpiration().after(new Date());
	}

	public long accessTtlSeconds() {
		return accessTtlSeconds;
	}

	private Claims claims(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private static byte[] sha256(String secret) {
		try {
			return MessageDigest.getInstance("SHA-256")
					.digest(secret.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}
}
