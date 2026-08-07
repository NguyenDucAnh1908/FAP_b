package com.fap.training.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.ForbiddenException;
import com.fap.common.exception.NotFoundException;
import com.fap.training.entity.AttendanceRecord;
import com.fap.training.entity.TrainingRegistration;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.AttendanceCheckInMethod;
import com.fap.training.enums.AttendanceStatus;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.mapper.AttendanceRecordMapper;
import com.fap.training.repository.AttendanceRecordRepository;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fap.training.repository.TrainingSessionRepository;
import com.fap.user.entity.User;
import com.fap.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * QR self check-in is the one attendance path a trainee drives themselves, so the registration
 * probe is the only thing standing between "scan the poster" and "mark anybody present". These
 * tests pin that probe down: it must ask for {@code Registered} specifically, not merely for the
 * existence of a registration row.
 */
class AttendanceCheckInTest {

	private static final long SESSION_ID = 55L;
	private static final long TRAINEE_ID = 900L;

	private final TrainingSessionRepository trainingSessionRepository = mock(TrainingSessionRepository.class);
	private final TrainingRegistrationRepository trainingRegistrationRepository =
			mock(TrainingRegistrationRepository.class);
	private final AttendanceRecordRepository attendanceRecordRepository = mock(AttendanceRecordRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final AttendanceRecordMapper attendanceRecordMapper = mock(AttendanceRecordMapper.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);

	private final AttendanceService service = new AttendanceService(
			trainingSessionRepository,
			trainingRegistrationRepository,
			attendanceRecordRepository,
			userRepository,
			attendanceRecordMapper,
			auditLogService);

	@Test
	void createsPresentRecordForRegisteredTrainee() {
		TrainingSession session = givenSession(TrainingSessionStatus.Upcoming);
		givenRegistration(TrainingRegistrationStatus.Registered);
		when(attendanceRecordRepository.findByTrainingSessionIdAndUserId(SESSION_ID, TRAINEE_ID))
				.thenReturn(Optional.empty());

		service.checkIn(SESSION_ID, TRAINEE_ID);

		AttendanceRecord saved = captureSavedRecord();
		assertThat(saved.getStatus()).isEqualTo(AttendanceStatus.Present);
		assertThat(saved.getCheckInMethod()).isEqualTo(AttendanceCheckInMethod.QR);
		assertThat(saved.getCheckedInAt()).isNotNull();
		assertThat(saved.getUpdatedBy()).isEqualTo(TRAINEE_ID);
		assertThat(saved.getTrainingSession()).isSameAs(session);
		assertThat(saved.getUser().getId()).isEqualTo(TRAINEE_ID);
		verify(auditLogService).record("QR_CHECK_IN", "training_session", SESSION_ID);
	}

	/**
	 * Scanning twice must not create a second row — the unique constraint on
	 * {@code (training_id, user_id)} would reject it, so the second scan updates in place.
	 */
	@Test
	void secondScanUpdatesExistingRecordInsteadOfCreatingAnother() {
		givenSession(TrainingSessionStatus.Upcoming);
		givenRegistration(TrainingRegistrationStatus.Registered);
		AttendanceRecord existing = new AttendanceRecord();
		existing.setId(7L);
		existing.setStatus(AttendanceStatus.Absent);
		existing.setCheckInMethod(AttendanceCheckInMethod.Manual);
		when(attendanceRecordRepository.findByTrainingSessionIdAndUserId(SESSION_ID, TRAINEE_ID))
				.thenReturn(Optional.of(existing));

		service.checkIn(SESSION_ID, TRAINEE_ID);

		AttendanceRecord saved = captureSavedRecord();
		assertThat(saved).isSameAs(existing);
		assertThat(saved.getStatus()).isEqualTo(AttendanceStatus.Present);
		assertThat(saved.getCheckInMethod()).isEqualTo(AttendanceCheckInMethod.QR);
	}

	/**
	 * The core authorization case. A trainee who never registered, cancelled, or is only waitlisted
	 * has no claim on this session, and the service must not fall back to creating a record.
	 */
	@ParameterizedTest(name = "{0} registration cannot check in")
	@EnumSource(value = TrainingRegistrationStatus.class,
			names = {"Waitlist", "Cancelled", "Completed"})
	void rejectsTraineeWhoseRegistrationIsNotRegistered(TrainingRegistrationStatus status) {
		givenSession(TrainingSessionStatus.Upcoming);
		// The service filters on Registered in the query, so any other status yields no row at all.
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserIdAndStatus(
				SESSION_ID, TRAINEE_ID, TrainingRegistrationStatus.Registered))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.checkIn(SESSION_ID, TRAINEE_ID))
				.isInstanceOf(ForbiddenException.class);

		verify(attendanceRecordRepository, never()).save(any());
		verifyNoInteractions(auditLogService);
	}

	@Test
	void rejectsTraineeWithNoRegistrationAtAll() {
		givenSession(TrainingSessionStatus.Upcoming);
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserIdAndStatus(
				anyLong(), anyLong(), any()))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.checkIn(SESSION_ID, TRAINEE_ID))
				.isInstanceOf(ForbiddenException.class);

		verify(attendanceRecordRepository, never()).save(any());
	}

	@Test
	void rejectsCheckInToCanceledSession() {
		givenSession(TrainingSessionStatus.Canceled);

		assertThatThrownBy(() -> service.checkIn(SESSION_ID, TRAINEE_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("ATTENDANCE_SESSION_CANCELED");

		// Session status is checked before the registration probe, so no lookup should happen.
		verifyNoInteractions(trainingRegistrationRepository);
		verify(attendanceRecordRepository, never()).save(any());
	}

	/**
	 * Completed sessions are closed to self check-in. Late corrections go through the staff-only
	 * upsert path, which demands a correction reason and records a distinct audit action.
	 */
	@Test
	void rejectsCheckInToCompletedSession() {
		givenSession(TrainingSessionStatus.Completed);

		assertThatThrownBy(() -> service.checkIn(SESSION_ID, TRAINEE_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("ATTENDANCE_SESSION_COMPLETED");

		verifyNoInteractions(trainingRegistrationRepository);
		verify(attendanceRecordRepository, never()).save(any());
	}

	@Test
	void rejectsUnknownSession() {
		when(trainingSessionRepository.findWithClassAndTrainerById(SESSION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.checkIn(SESSION_ID, TRAINEE_ID))
				.isInstanceOf(NotFoundException.class);

		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	private TrainingSession givenSession(TrainingSessionStatus status) {
		TrainingSession session = new TrainingSession();
		session.setId(SESSION_ID);
		session.setTitle("Oracle tuning workshop");
		session.setStatus(status);
		when(trainingSessionRepository.findWithClassAndTrainerById(SESSION_ID)).thenReturn(Optional.of(session));
		return session;
	}

	private void givenRegistration(TrainingRegistrationStatus status) {
		User user = new User();
		user.setId(TRAINEE_ID);
		user.setFullName("Trainee");
		TrainingRegistration registration = new TrainingRegistration();
		registration.setId(300L);
		registration.setUser(user);
		registration.setStatus(status);
		registration.setRegisteredAt(LocalDateTime.now().minusDays(1));
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserIdAndStatus(
				SESSION_ID, TRAINEE_ID, TrainingRegistrationStatus.Registered))
				.thenReturn(Optional.of(registration));
	}

	private AttendanceRecord captureSavedRecord() {
		ArgumentCaptor<AttendanceRecord> captor = ArgumentCaptor.forClass(AttendanceRecord.class);
		verify(attendanceRecordRepository).save(captor.capture());
		return captor.getValue();
	}
}
