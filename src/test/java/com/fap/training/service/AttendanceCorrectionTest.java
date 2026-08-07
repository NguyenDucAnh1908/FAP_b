package com.fap.training.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.ForbiddenException;
import com.fap.common.exception.NotFoundException;
import com.fap.training.dto.AttendanceItemRequest;
import com.fap.training.dto.UpdateAttendanceRequest;
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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Attendance edited after a session is Completed is a correction, not ordinary data entry: every
 * record must carry a reason and the audit trail must say so. Before completion no reason is asked
 * for, because nothing is being corrected yet.
 */
class AttendanceCorrectionTest {

	private static final long SESSION_ID = 55L;
	private static final long TRAINEE_ID = 900L;
	private static final long STAFF_ID = 7L;

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
	void recordsPlainUpsertAuditActionWhenSessionIsStillUpcoming() {
		givenSession(TrainingSessionStatus.Upcoming);
		givenTraineeIsRegistered();

		service.upsert(SESSION_ID, request(item(AttendanceStatus.Present, null)), STAFF_ID);

		verify(auditLogService).record("UPSERT_ATTENDANCE", "training_session", SESSION_ID);
	}

	@Test
	void allowsMissingCorrectionReasonBeforeCompletion() {
		givenSession(TrainingSessionStatus.Upcoming);
		givenTraineeIsRegistered();

		assertThatCode(() -> service.upsert(SESSION_ID, request(item(AttendanceStatus.Present, null)), STAFF_ID))
				.doesNotThrowAnyException();
	}

	@ParameterizedTest(name = "post-completion edit with reason [{0}] is rejected")
	@NullAndEmptySource
	@ValueSource(strings = {" ", "   ", "\t"})
	void rejectsPostCompletionEditWithoutCorrectionReason(String reason) {
		givenSession(TrainingSessionStatus.Completed);
		givenTraineeIsRegistered();

		assertThatThrownBy(() ->
				service.upsert(SESSION_ID, request(item(AttendanceStatus.Present, reason)), STAFF_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("ATTENDANCE_CORRECTION_REASON_REQUIRED");

		verify(attendanceRecordRepository, never()).saveAll(any());
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	/**
	 * The reason is required per record, not per request — one filled-in reason must not license
	 * editing somebody else's attendance silently.
	 */
	@Test
	void rejectsPostCompletionEditWhenOnlySomeRecordsCarryAReason() {
		givenSession(TrainingSessionStatus.Completed);
		givenTraineeIsRegistered();
		UpdateAttendanceRequest request = new UpdateAttendanceRequest(List.of(
				new AttendanceItemRequest(TRAINEE_ID, AttendanceStatus.Present, null, null, "Scanner was offline"),
				new AttendanceItemRequest(901L, AttendanceStatus.Absent, null, null, null)));

		assertThatThrownBy(() -> service.upsert(SESSION_ID, request, STAFF_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("ATTENDANCE_CORRECTION_REASON_REQUIRED");
	}

	@Test
	void allowsPostCompletionEditWithReasonAndRecordsCorrectionAuditAction() {
		givenSession(TrainingSessionStatus.Completed);
		givenTraineeIsRegistered();

		service.upsert(SESSION_ID, request(item(AttendanceStatus.Present, "Scanner was offline")), STAFF_ID);

		verify(auditLogService).record("UPSERT_ATTENDANCE_CORRECTION", "training_session", SESSION_ID);
		verify(auditLogService, never()).record("UPSERT_ATTENDANCE", "training_session", SESSION_ID);
	}

	@Test
	void rejectsAnyUpsertOnCanceledSession() {
		givenSession(TrainingSessionStatus.Canceled);

		assertThatThrownBy(() ->
				service.upsert(SESSION_ID, request(item(AttendanceStatus.Present, "reason")), STAFF_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("ATTENDANCE_SESSION_CANCELED");
	}

	// --- QR self check-in -------------------------------------------------------------------

	@Test
	void checkInMarksRegisteredTraineePresentViaQr() {
		TrainingSession session = givenSession(TrainingSessionStatus.Upcoming);
		givenTraineeRegistrationLookupSucceeds();
		when(attendanceRecordRepository.findByTrainingSessionIdAndUserId(SESSION_ID, TRAINEE_ID))
				.thenReturn(Optional.empty());

		service.checkIn(SESSION_ID, TRAINEE_ID);

		AttendanceRecord saved = captureSavedRecord();
		assertThat(saved.getStatus()).isEqualTo(AttendanceStatus.Present);
		assertThat(saved.getCheckInMethod()).isEqualTo(AttendanceCheckInMethod.QR);
		assertThat(saved.getCheckedInAt()).isNotNull();
		assertThat(saved.getUpdatedBy()).isEqualTo(TRAINEE_ID);
		assertThat(saved.getTrainingSession()).isSameAs(session);
		verify(auditLogService).record("QR_CHECK_IN", "training_session", SESSION_ID);
	}

	/** Scanning twice must not create a second row, and must not flip a Present record to something else. */
	@Test
	void checkInIsIdempotentWhenRecordAlreadyExists() {
		givenSession(TrainingSessionStatus.Upcoming);
		givenTraineeRegistrationLookupSucceeds();
		AttendanceRecord existing = new AttendanceRecord();
		existing.setId(4242L);
		existing.setStatus(AttendanceStatus.Absent);
		when(attendanceRecordRepository.findByTrainingSessionIdAndUserId(SESSION_ID, TRAINEE_ID))
				.thenReturn(Optional.of(existing));

		service.checkIn(SESSION_ID, TRAINEE_ID);

		AttendanceRecord saved = captureSavedRecord();
		assertThat(saved).isSameAs(existing);
		assertThat(saved.getStatus()).isEqualTo(AttendanceStatus.Present);
		assertThat(saved.getCheckInMethod()).isEqualTo(AttendanceCheckInMethod.QR);
	}

	/**
	 * Only a {@code Registered} registration may scan in. The repository lookup filters on status, so
	 * Waitlist, Cancelled, and Completed all surface as "no registration" and must become a 403.
	 */
	@ParameterizedTest(name = "check-in without a Registered registration is forbidden ({0})")
	@EnumSource(value = TrainingRegistrationStatus.class,
			names = {"Waitlist", "Cancelled", "Completed"})
	void checkInRejectsTraineeWithoutRegisteredRegistration(TrainingRegistrationStatus ineligible) {
		givenSession(TrainingSessionStatus.Upcoming);
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserIdAndStatus(
				SESSION_ID, TRAINEE_ID, TrainingRegistrationStatus.Registered))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.checkIn(SESSION_ID, TRAINEE_ID))
				.isInstanceOf(ForbiddenException.class);

		verify(attendanceRecordRepository, never()).save(any());
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@Test
	void checkInRejectsCanceledSession() {
		givenSession(TrainingSessionStatus.Canceled);

		assertThatThrownBy(() -> service.checkIn(SESSION_ID, TRAINEE_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("ATTENDANCE_SESSION_CANCELED");
	}

	@Test
	void checkInRejectsCompletedSession() {
		givenSession(TrainingSessionStatus.Completed);

		assertThatThrownBy(() -> service.checkIn(SESSION_ID, TRAINEE_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("ATTENDANCE_SESSION_COMPLETED");
	}

	@Test
	void checkInRejectsUnknownSession() {
		when(trainingSessionRepository.findWithClassAndTrainerById(SESSION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.checkIn(SESSION_ID, TRAINEE_ID))
				.isInstanceOf(NotFoundException.class);
	}

	// --- fixtures ---------------------------------------------------------------------------

	private TrainingSession givenSession(TrainingSessionStatus status) {
		TrainingSession session = new TrainingSession();
		session.setId(SESSION_ID);
		session.setTitle("Concurrency in practice");
		session.setStatus(status);
		when(trainingSessionRepository.findWithClassAndTrainerById(SESSION_ID)).thenReturn(Optional.of(session));
		return session;
	}

	private void givenTraineeIsRegistered() {
		User trainee = trainee();
		TrainingRegistration registration = new TrainingRegistration();
		registration.setId(1L);
		registration.setUser(trainee);
		registration.setStatus(TrainingRegistrationStatus.Registered);
		when(trainingRegistrationRepository.findByTrainingSessionIdAndStatusInOrderByRegisteredAtAscIdAsc(
				anyLong(), any()))
				.thenReturn(List.of(registration));
		when(userRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee));
		when(attendanceRecordRepository.findByTrainingSessionIdAndUserId(SESSION_ID, TRAINEE_ID))
				.thenReturn(Optional.empty());
	}

	private void givenTraineeRegistrationLookupSucceeds() {
		TrainingRegistration registration = new TrainingRegistration();
		registration.setId(1L);
		registration.setUser(trainee());
		registration.setStatus(TrainingRegistrationStatus.Registered);
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserIdAndStatus(
				SESSION_ID, TRAINEE_ID, TrainingRegistrationStatus.Registered))
				.thenReturn(Optional.of(registration));
	}

	private User trainee() {
		User user = new User();
		user.setId(TRAINEE_ID);
		user.setFullName("Trainee One");
		return user;
	}

	private AttendanceRecord captureSavedRecord() {
		org.mockito.ArgumentCaptor<AttendanceRecord> captor =
				org.mockito.ArgumentCaptor.forClass(AttendanceRecord.class);
		verify(attendanceRecordRepository).save(captor.capture());
		return captor.getValue();
	}

	private UpdateAttendanceRequest request(AttendanceItemRequest item) {
		return new UpdateAttendanceRequest(List.of(item));
	}

	private AttendanceItemRequest item(AttendanceStatus status, String correctionReason) {
		LocalDateTime checkedInAt = status == AttendanceStatus.Absent ? null : LocalDateTime.now();
		return new AttendanceItemRequest(TRAINEE_ID, status, checkedInAt, null, correctionReason);
	}
}
