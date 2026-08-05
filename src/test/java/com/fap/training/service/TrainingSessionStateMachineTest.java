package com.fap.training.service;

import com.fap.clazz.repository.ClassRepository;
import com.fap.clazz.repository.ClassTrainerRepository;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.notification.service.NotificationService;
import com.fap.training.entity.TrainingRegistration;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.mapper.TrainingSessionMapper;
import com.fap.training.repository.AttendanceRecordRepository;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fap.training.repository.TrainingSessionRepository;
import com.fap.user.entity.User;
import com.fap.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Guards the training session lifecycle: Upcoming is the only status you can leave, and it leads
 * to either Completed or Canceled. Completion additionally requires attendance to be recorded for
 * everyone still registered, and it promotes those registrations to Completed.
 */
class TrainingSessionStateMachineTest {

	private static final long SESSION_ID = 41L;
	private static final long CURRENT_USER_ID = 7L;

	private final TrainingSessionRepository trainingSessionRepository = mock(TrainingSessionRepository.class);
	private final TrainingRegistrationRepository trainingRegistrationRepository =
			mock(TrainingRegistrationRepository.class);
	private final AttendanceRecordRepository attendanceRecordRepository = mock(AttendanceRecordRepository.class);
	private final ClassRepository classRepository = mock(ClassRepository.class);
	private final ClassTrainerRepository classTrainerRepository = mock(ClassTrainerRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final TrainingSessionMapper trainingSessionMapper = mock(TrainingSessionMapper.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final NotificationService notificationService = mock(NotificationService.class);

	private final TrainingSessionService service = new TrainingSessionService(
			trainingSessionRepository,
			trainingRegistrationRepository,
			attendanceRecordRepository,
			classRepository,
			classTrainerRepository,
			userRepository,
			trainingSessionMapper,
			auditLogService,
			notificationService);

	@ParameterizedTest(name = "Upcoming -> {0} is allowed")
	@CsvSource({"Completed", "Canceled"})
	void allowsValidTransition(TrainingSessionStatus target) {
		TrainingSession session = givenSession(TrainingSessionStatus.Upcoming);
		givenAttendanceIsComplete();

		service.updateStatus(SESSION_ID, target, CURRENT_USER_ID);

		assertThat(session.getStatus()).isEqualTo(target);
		assertThat(session.getUpdatedBy()).isEqualTo(CURRENT_USER_ID);
		verify(auditLogService).record(
				"UPDATE_TRAINING_SESSION_STATUS:" + target.name(), "training_session", SESSION_ID);
	}

	@ParameterizedTest(name = "{0} -> {1} is rejected")
	@CsvSource({
			"Completed, Upcoming",
			"Completed, Canceled",
			"Canceled, Upcoming",
			"Canceled, Completed"
	})
	void rejectsInvalidTransition(TrainingSessionStatus current, TrainingSessionStatus target) {
		TrainingSession session = givenSession(current);
		givenAttendanceIsComplete();

		assertThatThrownBy(() -> service.updateStatus(SESSION_ID, target, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("INVALID_TRAINING_SESSION_STATUS_TRANSITION");

		assertThat(session.getStatus()).isEqualTo(current);
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
		verifyNoInteractions(notificationService);
	}

	@ParameterizedTest(name = "{0} -> {0} is a no-op")
	@CsvSource({"Upcoming", "Completed", "Canceled"})
	void allowsTransitionToSameStatus(TrainingSessionStatus current) {
		TrainingSession session = givenSession(current);
		givenAttendanceIsComplete();

		service.updateStatus(SESSION_ID, current, CURRENT_USER_ID);

		assertThat(session.getStatus()).isEqualTo(current);
	}

	@Test
	void rejectsCompletionWhenAttendanceIsMissingForSomeRegistrations() {
		TrainingSession session = givenSession(TrainingSessionStatus.Upcoming);
		when(trainingRegistrationRepository.countByTrainingSessionIdAndStatus(
				SESSION_ID, TrainingRegistrationStatus.Registered)).thenReturn(3L);
		when(attendanceRecordRepository.countByTrainingSessionId(SESSION_ID)).thenReturn(2L);

		assertThatThrownBy(() ->
				service.updateStatus(SESSION_ID, TrainingSessionStatus.Completed, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("TRAINING_SESSION_ATTENDANCE_REQUIRED");

		assertThat(session.getStatus()).isEqualTo(TrainingSessionStatus.Upcoming);
	}

	/**
	 * A session nobody registered for still has to be completable — the attendance guard only
	 * applies when there is somebody whose attendance could have been recorded.
	 */
	@Test
	void allowsCompletionWhenNobodyIsRegistered() {
		TrainingSession session = givenSession(TrainingSessionStatus.Upcoming);
		when(trainingRegistrationRepository.countByTrainingSessionIdAndStatus(
				SESSION_ID, TrainingRegistrationStatus.Registered)).thenReturn(0L);
		when(attendanceRecordRepository.countByTrainingSessionId(SESSION_ID)).thenReturn(0L);

		service.updateStatus(SESSION_ID, TrainingSessionStatus.Completed, CURRENT_USER_ID);

		assertThat(session.getStatus()).isEqualTo(TrainingSessionStatus.Completed);
	}

	@Test
	void completionPromotesRegisteredParticipantsToCompleted() {
		givenSession(TrainingSessionStatus.Upcoming);
		givenAttendanceIsComplete();
		TrainingRegistration registration = registration(101L, TrainingRegistrationStatus.Registered);
		when(trainingRegistrationRepository.findByTrainingSessionIdAndStatusInOrderByRegisteredAtAscIdAsc(
				eq(SESSION_ID), any()))
				.thenReturn(List.of(registration));

		service.updateStatus(SESSION_ID, TrainingSessionStatus.Completed, CURRENT_USER_ID);

		assertThat(registration.getStatus()).isEqualTo(TrainingRegistrationStatus.Completed);
		assertThat(registration.getCompletedAt()).isNotNull();
		verify(notificationService).create(eq(101L), anyString(), anyString());
	}

	/**
	 * Cancellation must not touch registration statuses — a canceled session was never delivered,
	 * so nobody completed it.
	 */
	@Test
	void cancellationNotifiesParticipantsWithoutCompletingRegistrations() {
		givenSession(TrainingSessionStatus.Upcoming);
		TrainingRegistration registration = registration(102L, TrainingRegistrationStatus.Registered);
		when(trainingRegistrationRepository.findByTrainingSessionIdAndStatusInOrderByRegisteredAtAscIdAsc(
				eq(SESSION_ID), any()))
				.thenReturn(List.of(registration));

		service.updateStatus(SESSION_ID, TrainingSessionStatus.Canceled, CURRENT_USER_ID);

		assertThat(registration.getStatus()).isEqualTo(TrainingRegistrationStatus.Registered);
		assertThat(registration.getCompletedAt()).isNull();
		verify(notificationService).create(eq(102L), anyString(), anyString());
	}

	private TrainingSession givenSession(TrainingSessionStatus status) {
		TrainingSession session = new TrainingSession();
		session.setId(SESSION_ID);
		session.setTitle("Spring Boot fundamentals");
		session.setStatus(status);
		when(trainingSessionRepository.findWithClassAndTrainerById(SESSION_ID)).thenReturn(Optional.of(session));
		return session;
	}

	private void givenAttendanceIsComplete() {
		when(trainingRegistrationRepository.countByTrainingSessionIdAndStatus(
				SESSION_ID, TrainingRegistrationStatus.Registered)).thenReturn(2L);
		when(attendanceRecordRepository.countByTrainingSessionId(SESSION_ID)).thenReturn(2L);
	}

	private TrainingRegistration registration(Long userId, TrainingRegistrationStatus status) {
		User user = new User();
		user.setId(userId);
		TrainingRegistration registration = new TrainingRegistration();
		registration.setId(userId);
		registration.setUser(user);
		registration.setStatus(status);
		return registration;
	}
}
