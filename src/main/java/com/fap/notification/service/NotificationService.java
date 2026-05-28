package com.fap.notification.service;

import com.fap.common.exception.NotFoundException;
import com.fap.notification.dto.NotificationResponse;
import com.fap.notification.entity.Notification;
import com.fap.notification.mapper.NotificationMapper;
import com.fap.notification.repository.NotificationRepository;
import com.fap.user.entity.User;
import com.fap.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final NotificationMapper notificationMapper;

	public NotificationService(
			NotificationRepository notificationRepository,
			UserRepository userRepository,
			NotificationMapper notificationMapper) {
		this.notificationRepository = notificationRepository;
		this.userRepository = userRepository;
		this.notificationMapper = notificationMapper;
	}

	@Transactional(readOnly = true)
	public Page<NotificationResponse> listMyNotifications(Long userId, int page, int limit) {
		PageRequest pageRequest = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
		return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest)
				.map(notificationMapper::toResponse);
	}

	@Transactional
	public NotificationResponse markRead(Long notificationId, Long userId) {
		Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
				.orElseThrow(() -> new NotFoundException("Notification not found"));
		notification.setRead(true);
		return notificationMapper.toResponse(notification);
	}

	@Transactional
	public void create(Long userId, String title, String message) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found"));
		Notification notification = new Notification();
		notification.setUser(user);
		notification.setTitle(title);
		notification.setMessage(message);
		notification.setRead(false);
		notification.setCreatedAt(LocalDateTime.now());
		notificationRepository.save(notification);
	}
}
