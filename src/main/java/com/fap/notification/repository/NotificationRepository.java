package com.fap.notification.repository;

import com.fap.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	@EntityGraph(attributePaths = "user")
	Optional<Notification> findByIdAndUserId(Long id, Long userId);
}
