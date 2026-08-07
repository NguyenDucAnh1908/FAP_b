package com.fap.common.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guards the startup contract of the signing secret. A weak or absent secret cannot be allowed to
 * boot: {@code sha256} stretches any input to 32 bytes, so tokens would sign successfully and the
 * weakness would stay invisible until someone forged a token.
 */
class JwtServiceTest {

	private static final String VALID_SECRET = "0123456789abcdef0123456789abcdef";

	private static FapUserPrincipal principal() {
		return new FapUserPrincipal(
				7L,
				"trainer1@fap.edu.vn",
				"$2a$10$hash",
				Set.of("Trainer"),
				true,
				List.of(new SimpleGrantedAuthority("ROLE_Trainer")));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" ", "\t", "\n"})
	void rejectsMissingSecret(String secret) {
		assertThatThrownBy(() -> new JwtService(secret, 15))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("must be configured");
	}

	@ParameterizedTest
	@ValueSource(strings = {"short", "change_me", "change_me_dev_secret", "0123456789abcdef0123456789abcde"})
	void rejectsSecretShorterThanThirtyTwoCharacters(String secret) {
		assertThatThrownBy(() -> new JwtService(secret, 15))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("at least 32 characters");
	}

	@Test
	void acceptsSecretOfExactlyThirtyTwoCharacters() {
		assertThat(VALID_SECRET).hasSize(32);
		assertThat(new JwtService(VALID_SECRET, 15).accessTtlSeconds()).isEqualTo(900);
	}

	@Test
	void signsAndReadsBackTheSubject() {
		JwtService jwtService = new JwtService(VALID_SECRET, 15);
		String token = jwtService.generateAccessToken(principal());

		assertThat(jwtService.extractSubject(token)).isEqualTo("trainer1@fap.edu.vn");
		assertThat(jwtService.isValid(token, principal())).isTrue();
	}

	@Test
	void rejectsTokenSignedWithAnotherSecret() {
		String token = new JwtService(VALID_SECRET, 15).generateAccessToken(principal());
		JwtService otherKeyHolder = new JwtService("fedcba9876543210fedcba9876543210", 15);

		assertThatThrownBy(() -> otherKeyHolder.extractSubject(token))
				.isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
	}
}
