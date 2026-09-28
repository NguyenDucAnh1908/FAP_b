package com.fap.notification.service;

import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.NotFoundException;
import com.fap.notification.dto.NotificationResponse;
import com.fap.notification.entity.Notification;
import com.fap.notification.mapper.NotificationMapper;
import com.fap.notification.repository.NotificationRepository;
import com.fap.user.entity.User;
import com.fap.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The notification inbox is strictly per user: a trainee must never read or acknowledge somebody
 * else's notification, so ownership is part of every lookup. {@code create} is called in loops by
 * other modules, which is why it references the recipient instead of selecting it.
 */
class NotificationServiceTest {

	private static final long USER_ID = 7L;
	private static final long NOTIFICATION_ID = 91L;

	private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	// The real mapper keeps the assertions on what the API actually returns.
	private final NotificationMapper notificationMapper = new NotificationMapper();

	private final NotificationService service = new NotificationService(
			notificationRepository,
			userRepository,
			notificationMapper);

	@Test
	void listsOwnNotificationsNewestFirstByDefault() {
		Notification notification = notification(false);
		when(notificationRepository.findByUserId(eq(USER_ID), any()))
				.thenReturn(new PageImpl<>(List.of(notification)));

		Page<NotificationResponse> page = service.listMyNotifications(USER_ID, 2, 15);

		Pageable pageable = capturePageable();
		assertThat(pageable.getPageNumber()).isEqualTo(2);
		assertThat(pageable.getPageSize()).isEqualTo(15);
		assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
		assertThat(page.getContent())
				.extracting(NotificationResponse::id, NotificationResponse::title, NotificationResponse::read)
				.containsExactly(tuple(NOTIFICATION_ID, "Class opened", false));
	}

	@ParameterizedTest(name = "sort by {0} is allowed")
	@ValueSource(strings = {"id", "createdAt", "title", "read"})
	void listsWithWhitelistedSortField(String sortBy) {
		when(notificationRepository.findByUserId(eq(USER_ID), any())).thenReturn(Page.empty());

		service.listMyNotifications(USER_ID, 0, 20, sortBy, "desc");

		assertThat(capturePageable().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, sortBy));
	}

	@Test
	void listsAscendingWhenSortFieldHasNoOrder() {
		when(notificationRepository.findByUserId(eq(USER_ID), any())).thenReturn(Page.empty());

		service.listMyNotifications(USER_ID, 0, 20, " title ", null);

		assertThat(capturePageable().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "title"));
	}

	/** Sorting by an arbitrary property would let a caller probe columns such as the user relation. */
	@ParameterizedTest(name = "sort by {0} is rejected")
	@ValueSource(strings = {"message", "user", "user.email"})
	void rejectsSortFieldOutsideWhitelist(String sortBy) {
		assertThatThrownBy(() -> service.listMyNotifications(USER_ID, 0, 20, sortBy, "asc"))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_SORT_FIELD");

		verify(notificationRepository, never()).findByUserId(anyLong(), any());
	}

	@Test
	void rejectsUnknownSortOrder() {
		assertThatThrownBy(() -> service.listMyNotifications(USER_ID, 0, 20, "createdAt", "sideways"))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_SORT_ORDER");

		verify(notificationRepository, never()).findByUserId(anyLong(), any());
	}

	@Test
	void markReadFlagsOwnNotification() {
		Notification notification = notification(false);
		when(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, USER_ID))
				.thenReturn(Optional.of(notification));

		NotificationResponse response = service.markRead(NOTIFICATION_ID, USER_ID);

		assertThat(notification.isRead()).isTrue();
		assertThat(response.id()).isEqualTo(NOTIFICATION_ID);
		assertThat(response.read()).isTrue();
	}

	@Test
	void markReadIsIdempotentForAlreadyReadNotification() {
		Notification notification = notification(true);
		when(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, USER_ID))
				.thenReturn(Optional.of(notification));

		NotificationResponse response = service.markRead(NOTIFICATION_ID, USER_ID);

		assertThat(notification.isRead()).isTrue();
		assertThat(response.read()).isTrue();
	}

	/**
	 * Another user's notification is reported as missing rather than forbidden, so ids of foreign
	 * notifications cannot be discovered.
	 */
	@Test
	void markReadRejectsForeignNotificationAsNotFound() {
		long otherUserId = 8L;
		when(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, otherUserId))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.markRead(NOTIFICATION_ID, otherUserId))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Notification not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		verify(notificationRepository, never()).findById(any());
	}

	@Test
	void createStoresUnreadNotificationForReferencedUser() {
		User recipient = new User();
		recipient.setId(USER_ID);
		when(userRepository.getReferenceById(USER_ID)).thenReturn(recipient);
		LocalDateTime before = LocalDateTime.now();

		service.create(USER_ID, "Class opened", "Class JAVA-01 is now open");

		LocalDateTime after = LocalDateTime.now();
		ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(saved.capture());
		Notification notification = saved.getValue();
		assertThat(notification.getUser()).isSameAs(recipient);
		assertThat(notification.getTitle()).isEqualTo("Class opened");
		assertThat(notification.getMessage()).isEqualTo("Class JAVA-01 is now open");
		assertThat(notification.isRead()).isFalse();
		assertThat(notification.getCreatedAt()).isBetween(before, after);
		verify(userRepository, never()).findById(any());
		verify(userRepository, never()).findWithRolesById(any());
	}

	private Pageable capturePageable() {
		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(notificationRepository).findByUserId(eq(USER_ID), pageable.capture());
		return pageable.getValue();
	}

	private static Notification notification(boolean read) {
		Notification notification = new Notification();
		notification.setId(NOTIFICATION_ID);
		notification.setTitle("Class opened");
		notification.setMessage("Class JAVA-01 is now open");
		notification.setRead(read);
		notification.setCreatedAt(LocalDateTime.now().minusHours(1));
		return notification;
	}
}
