package com.fap.notification.service;

import com.fap.common.api.PageRequestFactory;
import com.fap.common.exception.NotFoundException;
import com.fap.notification.dto.NotificationResponse;
import com.fap.notification.entity.Notification;
import com.fap.notification.mapper.NotificationMapper;
import com.fap.notification.repository.NotificationRepository;
import com.fap.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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
		return listMyNotifications(userId, page, limit, null, null);
	}

	@Transactional(readOnly = true)
	public Page<NotificationResponse> listMyNotifications(
			Long userId,
			int page,
			int limit,
			String sortBy,
			String order) {
		PageRequest pageRequest = PageRequestFactory.create(
				page,
				limit,
				sortBy,
				order,
				Sort.by(Sort.Direction.DESC, "createdAt"),
				"id", "createdAt", "title", "read");
		return notificationRepository.findByUserId(userId, pageRequest)
				.map(notificationMapper::toResponse);
	}

	@Transactional
	public NotificationResponse markRead(Long notificationId, Long userId) {
		Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
				.orElseThrow(() -> new NotFoundException("notification", "Notification not found"));
		notification.setRead(true);
		return notificationMapper.toResponse(notification);
	}

	/**
	 * Stores {@code titleKey}/{@code messageKey} and {@code args} (names, codes and titles for the
	 * key's {@code {n}} placeholders); the text is rendered in the reader's language on read. The
	 * English rendering is stored as well, as the fallback for a key that later leaves the bundles
	 * and for anything reading the table directly.
	 *
	 * <p>Callers pass ids of users they just read (admins, trainers, registrants), often in a loop,
	 * so the user is referenced by id instead of re-selected per notification; the foreign key still
	 * rejects an unknown id at flush.
	 */
	@Transactional
	public void create(Long userId, String titleKey, String messageKey, Object... args) {
		// A null argument renders as an empty placeholder rather than the literal "null".
		List<String> texts = Arrays.stream(args)
				.map(arg -> arg == null ? "" : arg.toString())
				.toList();
		Notification notification = new Notification();
		notification.setUser(userRepository.getReferenceById(userId));
		notification.setTitleKey(titleKey);
		notification.setMessageKey(messageKey);
		notification.setMessageArgs(notificationMapper.writeArgs(texts));
		notification.setTitle(notificationMapper.render(Locale.ENGLISH, titleKey, titleKey, List.of()));
		notification.setMessage(notificationMapper.render(Locale.ENGLISH, messageKey, messageKey, texts));
		notification.setRead(false);
		notification.setCreatedAt(LocalDateTime.now());
		notificationRepository.save(notification);
	}
}
