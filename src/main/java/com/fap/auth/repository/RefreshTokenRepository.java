package com.fap.auth.repository;

import com.fap.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

	@Modifying
	@Query("update RefreshToken rt set rt.revoked = true where rt.user.id = :userId and rt.revoked = false")
	int revokeAllByUserId(@Param("userId") Long userId);
}
