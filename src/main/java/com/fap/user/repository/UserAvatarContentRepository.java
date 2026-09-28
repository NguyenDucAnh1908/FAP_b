package com.fap.user.repository;

import com.fap.common.exception.NotFoundException;
import com.fap.user.entity.UserAvatarContent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAvatarContentRepository extends JpaRepository<UserAvatarContent, Long> {

	/** One place for the lookup so every caller fails with the same not-found message. */
	default UserAvatarContent getAvatarContentOrThrow(Long userId) {
		return findById(userId)
				.orElseThrow(() -> new NotFoundException("user_avatar", "Avatar not found"));
	}
}
