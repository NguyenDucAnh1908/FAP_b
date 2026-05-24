package com.fap.auth.repository;

import com.fap.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

	Optional<PasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);

	@Modifying
	@Query("""
			update PasswordResetToken token
			set token.used = true,
			    token.usedAt = :usedAt
			where token.user.id = :userId
			  and token.used = false
			""")
	int markUnusedByUserIdAsUsed(@Param("userId") Long userId, @Param("usedAt") LocalDateTime usedAt);
}
