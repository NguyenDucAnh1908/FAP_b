package com.fap.notification.service;

import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.i18n.MessageService;
import com.fap.notification.dto.NotificationResponse;
import com.fap.notification.entity.Notification;
import com.fap.notification.mapper.NotificationMapper;
import com.fap.notification.repository.NotificationRepository;
import com.fap.user.entity.User;
import com.fap.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
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
 * other modules, which is why it references the recipient instead of selecting it, and it stores
 * message keys rather than text because the recipient's language is only known when they read.
 */
class NotificationServiceTest {

	private static final long USER_ID = 7L;
	private static final long NOTIFICATION_ID = 91L;
	private static final Locale VIETNAMESE = Locale.forLanguageTag("vi");
	private static final String TITLE_KEY = "notification.class_enrollment.promoted.title";
	private static final String MESSAGE_KEY = "notification.class_enrollment.promoted.message";

	private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	// The real mapper over a two-language bundle keeps the assertions on what the API actually returns.
	private final NotificationMapper notificationMapper = new NotificationMapper(
			new MessageService(messageSource()),
			new ObjectMapper());

	private final NotificationService service = new NotificationService(
			notificationRepository,
			userRepository,
			notificationMapper);

	@AfterEach
	void resetLocale() {
		LocaleContextHolder.resetLocaleContext();
	}

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

	@Test
	void listsKeyBasedNotificationsInTheReadersLanguage() {
		LocaleContextHolder.setLocale(VIETNAMESE);
		Notification notification = notification(false);
		notification.setTitleKey(TITLE_KEY);
		notification.setMessageKey(MESSAGE_KEY);
		notification.setMessageArgs("[\"Java Backend\"]");
		when(notificationRepository.findByUserId(eq(USER_ID), any()))
				.thenReturn(new PageImpl<>(List.of(notification)));

		Page<NotificationResponse> page = service.listMyNotifications(USER_ID, 0, 20);

		assertThat(page.getContent())
				.extracting(NotificationResponse::title, NotificationResponse::message)
				.containsExactly(tuple("Đã được chuyển từ danh sách chờ vào lớp", "Bạn đã được thêm vào lớp Java Backend"));
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
	void createStoresUnreadNotificationWithKeysAndArgumentsForReferencedUser() {
		User recipient = new User();
		recipient.setId(USER_ID);
		when(userRepository.getReferenceById(USER_ID)).thenReturn(recipient);
		LocalDateTime before = LocalDateTime.now();

		service.create(USER_ID, TITLE_KEY, MESSAGE_KEY, "Java Backend");

		LocalDateTime after = LocalDateTime.now();
		Notification notification = savedNotification();
		assertThat(notification.getUser()).isSameAs(recipient);
		assertThat(notification.getTitleKey()).isEqualTo(TITLE_KEY);
		assertThat(notification.getMessageKey()).isEqualTo(MESSAGE_KEY);
		assertThat(notification.getMessageArgs()).isEqualTo("[\"Java Backend\"]");
		assertThat(notification.isRead()).isFalse();
		assertThat(notification.getCreatedAt()).isBetween(before, after);
		verify(userRepository, never()).findById(any());
		verify(userRepository, never()).findWithRolesById(any());
	}

	/** The stored text is the English rendering whatever language the acting admin is using. */
	@Test
	void createStoresEnglishFallbackTextRegardlessOfRequestLocale() {
		LocaleContextHolder.setLocale(VIETNAMESE);
		when(userRepository.getReferenceById(USER_ID)).thenReturn(new User());

		service.create(USER_ID, TITLE_KEY, MESSAGE_KEY, "Java Backend");

		Notification notification = savedNotification();
		assertThat(notification.getTitle()).isEqualTo("Class waitlist promoted");
		assertThat(notification.getMessage()).isEqualTo("You have been enrolled in Java Backend");
	}

	@Test
	void createStoresEveryArgumentAsTextAndNullAsEmpty() {
		when(userRepository.getReferenceById(USER_ID)).thenReturn(new User());

		service.create(USER_ID, "notification.test.title", "notification.test.message", null, 42);

		Notification notification = savedNotification();
		assertThat(notification.getMessageArgs()).isEqualTo("[\"\",\"42\"]");
		assertThat(notification.getMessage()).isEqualTo("Args:  and 42");
	}

	/** A key missing from the bundles must not block the write; the key itself is kept as the text. */
	@Test
	void createKeepsTheKeyAsTextWhenItIsNotInTheBundles() {
		when(userRepository.getReferenceById(USER_ID)).thenReturn(new User());

		service.create(USER_ID, "notification.missing.title", "notification.missing.message", "Java Backend");

		Notification notification = savedNotification();
		assertThat(notification.getTitle()).isEqualTo("notification.missing.title");
		assertThat(notification.getMessage()).isEqualTo("notification.missing.message");
		assertThat(notification.getMessageArgs()).isEqualTo("[\"Java Backend\"]");
	}

	private Notification savedNotification() {
		ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(saved.capture());
		return saved.getValue();
	}

	private Pageable capturePageable() {
		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(notificationRepository).findByUserId(eq(USER_ID), pageable.capture());
		return pageable.getValue();
	}

	private static StaticMessageSource messageSource() {
		StaticMessageSource source = new StaticMessageSource();
		source.addMessage(TITLE_KEY, Locale.ENGLISH, "Class waitlist promoted");
		source.addMessage(MESSAGE_KEY, Locale.ENGLISH, "You have been enrolled in {0}");
		source.addMessage(TITLE_KEY, VIETNAMESE, "Đã được chuyển từ danh sách chờ vào lớp");
		source.addMessage(MESSAGE_KEY, VIETNAMESE, "Bạn đã được thêm vào lớp {0}");
		source.addMessage("notification.test.title", Locale.ENGLISH, "Test");
		source.addMessage("notification.test.message", Locale.ENGLISH, "Args: {0} and {1}");
		return source;
	}

	/** A row written before notifications carried keys: only the English text exists. */
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
