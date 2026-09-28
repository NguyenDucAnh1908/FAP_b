package com.fap.training.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.training.dto.AttendanceItemRequest;
import com.fap.training.dto.AttendanceRecordResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The staff attendance sheet and the roster read. AttendanceCheckInTest covers QR self check-in and
 * AttendanceCorrectionTest covers the post-completion reason rule; this class covers what the sheet
 * does to each record. Attendance feeds course results, so a record for somebody outside the
 * session, a duplicate row, or an absence with a check-in time would each distort a trainee's
 * attendance rate. The sheet is also all-or-nothing: one bad row must leave every row unsaved.
 */
class AttendanceServiceTest {

	private static final long SESSION_ID = 55L;
	private static final long TRAINEE_ID = 900L;
	private static final long SECOND_TRAINEE_ID = 901L;
	private static final long STAFF_ID = 7L;

	private final TrainingSessionRepository trainingSessionRepository = mock(TrainingSessionRepository.class);
	private final TrainingRegistrationRepository trainingRegistrationRepository =
			mock(TrainingRegistrationRepository.class);
	private final AttendanceRecordRepository attendanceRecordRepository = mock(AttendanceRecordRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);

	// The mapper has no collaborators, so the real one lets the tests assert the returned responses.
	private final AttendanceService service = new AttendanceService(
			trainingSessionRepository,
			trainingRegistrationRepository,
			attendanceRecordRepository,
			userRepository,
			new AttendanceRecordMapper(),
			auditLogService);

	@BeforeEach
	void delegateLookupToFinder() {
		lenient().doCallRealMethod().when(trainingSessionRepository).getWithClassAndTrainerOrThrow(any());
		lenient().doCallRealMethod().when(userRepository).getUserOrThrow(any());
	}

	// --- list -----------------------------------------------------------------------------------

	@Test
	void listReturnsRecordsInRepositoryOrder() {
		TrainingSession session = session(TrainingSessionStatus.Upcoming);
		when(trainingSessionRepository.existsById(SESSION_ID)).thenReturn(true);
		AttendanceRecord first = record(session, user(SECOND_TRAINEE_ID, "An Nguyen"), AttendanceStatus.Present);
		AttendanceRecord second = record(session, user(TRAINEE_ID, "Binh Tran"), AttendanceStatus.Absent);
		when(attendanceRecordRepository.findByTrainingSessionIdOrderByUserFullNameAsc(SESSION_ID))
				.thenReturn(List.of(first, second));

		List<AttendanceRecordResponse> responses = service.list(SESSION_ID);

		assertThat(responses)
				.extracting(AttendanceRecordResponse::userFullName, AttendanceRecordResponse::status)
				.containsExactly(
						tuple("An Nguyen", AttendanceStatus.Present),
						tuple("Binh Tran", AttendanceStatus.Absent));
		assertThat(responses).extracting(AttendanceRecordResponse::trainingSessionId).containsOnly(SESSION_ID);
	}

	@Test
	void listRejectsUnknownSession() {
		when(trainingSessionRepository.existsById(SESSION_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.list(SESSION_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Training session not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		verifyNoInteractions(attendanceRecordRepository);
	}

	// --- upsert: allowed ------------------------------------------------------------------------

	@Test
	void upsertCreatesManualRecordStampedWithNowWhenNoTimeOrMethodIsGiven() {
		TrainingSession session = givenSession(TrainingSessionStatus.Upcoming);
		User trainee = givenRegistered(TrainingRegistrationStatus.Registered, TRAINEE_ID);
		givenNoExistingRecord(TRAINEE_ID);
		LocalDateTime before = LocalDateTime.now();

		List<AttendanceRecordResponse> responses = service.upsert(
				SESSION_ID, request(item(TRAINEE_ID, AttendanceStatus.Present, null, null)), STAFF_ID);

		LocalDateTime after = LocalDateTime.now();
		AttendanceRecord saved = captureSavedRecords().get(0);
		assertThat(saved.getTrainingSession()).isSameAs(session);
		assertThat(saved.getUser()).isSameAs(trainee);
		assertThat(saved.getStatus()).isEqualTo(AttendanceStatus.Present);
		assertThat(saved.getCheckInMethod()).isEqualTo(AttendanceCheckInMethod.Manual);
		assertThat(saved.getCheckedInAt()).isBetween(before, after);
		assertThat(saved.getCreatedAt()).isEqualTo(saved.getCheckedInAt());
		assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCheckedInAt());
		assertThat(saved.getUpdatedBy()).isEqualTo(STAFF_ID);
		assertThat(saved.getCorrectionReason()).isNull();
		assertThat(responses)
				.singleElement()
				.satisfies(response -> {
					assertThat(response.userId()).isEqualTo(TRAINEE_ID);
					assertThat(response.status()).isEqualTo(AttendanceStatus.Present);
					assertThat(response.updatedBy()).isEqualTo(STAFF_ID);
				});
		verify(auditLogService).record("UPSERT_ATTENDANCE", "training_session", SESSION_ID);
	}

	@Test
	void upsertKeepsExplicitCheckInTimeAndMethod() {
		givenSession(TrainingSessionStatus.Upcoming);
		givenRegistered(TrainingRegistrationStatus.Registered, TRAINEE_ID);
		givenNoExistingRecord(TRAINEE_ID);
		LocalDateTime checkedInAt = LocalDateTime.of(2026, 9, 1, 9, 12);

		service.upsert(SESSION_ID, request(new AttendanceItemRequest(
				TRAINEE_ID, AttendanceStatus.Late, checkedInAt, AttendanceCheckInMethod.QR, null)), STAFF_ID);

		AttendanceRecord saved = captureSavedRecords().get(0);
		assertThat(saved.getStatus()).isEqualTo(AttendanceStatus.Late);
		assertThat(saved.getCheckedInAt()).isEqualTo(checkedInAt);
		assertThat(saved.getCheckInMethod()).isEqualTo(AttendanceCheckInMethod.QR);
	}

	/** Re-marking somebody absent must also wipe the check-in time an earlier edit left behind. */
	@Test
	void upsertMarkingAbsentClearsEarlierCheckInTime() {
		TrainingSession session = givenSession(TrainingSessionStatus.Upcoming);
		User trainee = givenRegistered(TrainingRegistrationStatus.Registered, TRAINEE_ID);
		AttendanceRecord existing = record(session, trainee, AttendanceStatus.Present);
		existing.setCheckedInAt(LocalDateTime.now().minusHours(1));
		when(attendanceRecordRepository.findByTrainingSessionIdAndUserId(SESSION_ID, TRAINEE_ID))
				.thenReturn(Optional.of(existing));

		service.upsert(SESSION_ID, request(item(TRAINEE_ID, AttendanceStatus.Absent, null, null)), STAFF_ID);

		AttendanceRecord saved = captureSavedRecords().get(0);
		assertThat(saved).isSameAs(existing);
		assertThat(saved.getStatus()).isEqualTo(AttendanceStatus.Absent);
		assertThat(saved.getCheckedInAt()).isNull();
	}

	/** The unique (session, user) constraint means an edit must update the row, never add a second. */
	@Test
	void upsertUpdatesExistingRecordInPlaceAndKeepsItsCreationTime() {
		TrainingSession session = givenSession(TrainingSessionStatus.Upcoming);
		User trainee = givenRegistered(TrainingRegistrationStatus.Registered, TRAINEE_ID);
		LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
		AttendanceRecord existing = record(session, trainee, AttendanceStatus.Absent);
		existing.setId(4242L);
		existing.setCreatedAt(createdAt);
		existing.setUpdatedBy(1L);
		when(attendanceRecordRepository.findByTrainingSessionIdAndUserId(SESSION_ID, TRAINEE_ID))
				.thenReturn(Optional.of(existing));

		service.upsert(SESSION_ID, request(item(TRAINEE_ID, AttendanceStatus.Present, null, null)), STAFF_ID);

		AttendanceRecord saved = captureSavedRecords().get(0);
		assertThat(saved).isSameAs(existing);
		assertThat(saved.getId()).isEqualTo(4242L);
		assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
		assertThat(saved.getStatus()).isEqualTo(AttendanceStatus.Present);
		assertThat(saved.getUpdatedBy()).isEqualTo(STAFF_ID);
		assertThat(saved.getUpdatedAt()).isAfter(createdAt);
	}

	/**
	 * Pins current behaviour: the sheet does not preserve QR provenance. When staff re-save a row
	 * without sending the method and time back, a QR check-in becomes Manual and its scan time is
	 * replaced by the save time.
	 */
	@Test
	void upsertWithoutMethodOrTimeReplacesEarlierQrCheckInDetails() {
		TrainingSession session = givenSession(TrainingSessionStatus.Upcoming);
		User trainee = givenRegistered(TrainingRegistrationStatus.Registered, TRAINEE_ID);
		LocalDateTime scannedAt = LocalDateTime.now().minusHours(2);
		AttendanceRecord existing = record(session, trainee, AttendanceStatus.Present);
		existing.setCheckInMethod(AttendanceCheckInMethod.QR);
		existing.setCheckedInAt(scannedAt);
		when(attendanceRecordRepository.findByTrainingSessionIdAndUserId(SESSION_ID, TRAINEE_ID))
				.thenReturn(Optional.of(existing));

		service.upsert(SESSION_ID, request(item(TRAINEE_ID, AttendanceStatus.Present, null, null)), STAFF_ID);

		AttendanceRecord saved = captureSavedRecords().get(0);
		assertThat(saved.getCheckInMethod()).isEqualTo(AttendanceCheckInMethod.Manual);
		assertThat(saved.getCheckedInAt()).isAfter(scannedAt);
	}

	@Test
	void upsertSavesEveryRowInOneBatchAndAnswersInRequestOrder() {
		givenSession(TrainingSessionStatus.Upcoming);
		givenRegistered(TrainingRegistrationStatus.Registered, TRAINEE_ID, SECOND_TRAINEE_ID);
		givenNoExistingRecord(TRAINEE_ID);
		givenNoExistingRecord(SECOND_TRAINEE_ID);

		List<AttendanceRecordResponse> responses = service.upsert(SESSION_ID, new UpdateAttendanceRequest(List.of(
				item(SECOND_TRAINEE_ID, AttendanceStatus.Late, null, null),
				item(TRAINEE_ID, AttendanceStatus.Absent, null, null))), STAFF_ID);

		assertThat(captureSavedRecords())
				.extracting(record -> record.getUser().getId())
				.containsExactly(SECOND_TRAINEE_ID, TRAINEE_ID);
		assertThat(responses)
				.extracting(AttendanceRecordResponse::userId)
				.containsExactly(SECOND_TRAINEE_ID, TRAINEE_ID);
		verify(auditLogService).record("UPSERT_ATTENDANCE", "training_session", SESSION_ID);
	}

	@Test
	void upsertStoresCorrectionReasonOnPostCompletionCorrection() {
		givenSession(TrainingSessionStatus.Completed);
		givenRegistered(TrainingRegistrationStatus.Completed, TRAINEE_ID);
		givenNoExistingRecord(TRAINEE_ID);

		service.upsert(SESSION_ID, request(new AttendanceItemRequest(
				TRAINEE_ID, AttendanceStatus.Present, null, null, "Scanner was offline")), STAFF_ID);

		assertThat(captureSavedRecords().get(0).getCorrectionReason()).isEqualTo("Scanner was offline");
	}

	// --- upsert: who may appear on the sheet ----------------------------------------------------

	/** Before completion the roster is the Registered trainees; Waitlist and Cancelled are left out. */
	@Test
	void upsertBeforeCompletionOnlyAcceptsRegisteredTrainees() {
		givenSession(TrainingSessionStatus.Upcoming);
		givenRegistered(TrainingRegistrationStatus.Registered, TRAINEE_ID);
		givenNoExistingRecord(TRAINEE_ID);

		service.upsert(SESSION_ID, request(item(TRAINEE_ID, AttendanceStatus.Present, null, null)), STAFF_ID);

		verify(trainingRegistrationRepository).findByTrainingSessionIdAndStatusInOrderByRegisteredAtAscIdAsc(
				SESSION_ID, List.of(TrainingRegistrationStatus.Registered));
	}

	@Test
	void upsertRejectsUserWhoIsNotOnTheRoster() {
		givenSession(TrainingSessionStatus.Upcoming);
		givenRegistered(TrainingRegistrationStatus.Registered, TRAINEE_ID);

		assertThatThrownBy(() -> service.upsert(
				SESSION_ID, request(item(SECOND_TRAINEE_ID, AttendanceStatus.Present, null, null)), STAFF_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("ATTENDANCE_USER_NOT_REGISTERED");

		verify(attendanceRecordRepository, never()).saveAll(any());
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	/**
	 * Once the session is Completed only Completed registrations can be corrected; a registration
	 * still in Registered was never completed and is treated as off the roster.
	 */
	@Test
	void upsertAfterCompletionRejectsTraineeWhoseRegistrationWasNotCompleted() {
		givenSession(TrainingSessionStatus.Completed);
		when(trainingRegistrationRepository.findByTrainingSessionIdAndStatusInOrderByRegisteredAtAscIdAsc(
				SESSION_ID, List.of(TrainingRegistrationStatus.Registered)))
				.thenReturn(List.of(registration(user(TRAINEE_ID, "Trainee"), TrainingRegistrationStatus.Registered)));
		when(trainingRegistrationRepository.findByTrainingSessionIdAndStatusInOrderByRegisteredAtAscIdAsc(
				SESSION_ID, List.of(TrainingRegistrationStatus.Completed)))
				.thenReturn(List.of());

		assertThatThrownBy(() -> service.upsert(SESSION_ID, request(new AttendanceItemRequest(
				TRAINEE_ID, AttendanceStatus.Present, null, null, "Arrived late")), STAFF_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("ATTENDANCE_USER_NOT_REGISTERED");

		verify(attendanceRecordRepository, never()).saveAll(any());
	}

	@Test
	void upsertRejectsDuplicateUserBeforeLoadingTheRoster() {
		givenSession(TrainingSessionStatus.Upcoming);

		assertThatThrownBy(() -> service.upsert(SESSION_ID, new UpdateAttendanceRequest(List.of(
				item(TRAINEE_ID, AttendanceStatus.Present, null, null),
				item(TRAINEE_ID, AttendanceStatus.Absent, null, null))), STAFF_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("DUPLICATE_ATTENDANCE_USER");

		verifyNoInteractions(trainingRegistrationRepository, userRepository, attendanceRecordRepository);
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	// --- upsert: rejected rows ------------------------------------------------------------------

	@ParameterizedTest(name = "{0} session: absent with a check-in time is rejected")
	@EnumSource(value = TrainingSessionStatus.class, names = {"Upcoming", "Completed"})
	void upsertRejectsAbsentRecordWithCheckInTime(TrainingSessionStatus status) {
		givenSession(status);
		TrainingRegistrationStatus rosterStatus = status == TrainingSessionStatus.Completed
				? TrainingRegistrationStatus.Completed
				: TrainingRegistrationStatus.Registered;
		givenRegistered(rosterStatus, TRAINEE_ID);

		assertThatThrownBy(() -> service.upsert(SESSION_ID, request(new AttendanceItemRequest(
				TRAINEE_ID, AttendanceStatus.Absent, LocalDateTime.now(), null, "Recorded by mistake")), STAFF_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("ABSENT_ATTENDANCE_CHECK_IN_NOT_ALLOWED");

		verify(attendanceRecordRepository, never()).saveAll(any());
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	/** The sheet is all-or-nothing: a valid first row must not be saved when a later row fails. */
	@Test
	void upsertSavesNothingWhenAnyRowIsInvalid() {
		givenSession(TrainingSessionStatus.Upcoming);
		givenRegistered(TrainingRegistrationStatus.Registered, TRAINEE_ID);
		givenNoExistingRecord(TRAINEE_ID);

		assertThatThrownBy(() -> service.upsert(SESSION_ID, new UpdateAttendanceRequest(List.of(
				item(TRAINEE_ID, AttendanceStatus.Present, null, null),
				item(SECOND_TRAINEE_ID, AttendanceStatus.Present, null, null))), STAFF_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("ATTENDANCE_USER_NOT_REGISTERED");

		verify(attendanceRecordRepository, never()).saveAll(any());
		verify(attendanceRecordRepository, never()).save(any());
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	/**
	 * Pins current behaviour: the user is re-read by id even though the registration already carries
	 * it, so a registration whose user row is missing surfaces as a not-found.
	 */
	@Test
	void upsertRejectsRegisteredUserWhoseUserRowIsMissing() {
		givenSession(TrainingSessionStatus.Upcoming);
		when(trainingRegistrationRepository.findByTrainingSessionIdAndStatusInOrderByRegisteredAtAscIdAsc(
				SESSION_ID, List.of(TrainingRegistrationStatus.Registered)))
				.thenReturn(List.of(registration(user(TRAINEE_ID, "Trainee"), TrainingRegistrationStatus.Registered)));
		when(userRepository.findById(TRAINEE_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.upsert(
				SESSION_ID, request(item(TRAINEE_ID, AttendanceStatus.Present, null, null)), STAFF_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("User not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		verify(attendanceRecordRepository, never()).saveAll(any());
	}

	@Test
	void upsertRejectsUnknownSession() {
		when(trainingSessionRepository.findWithClassAndTrainerById(SESSION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.upsert(
				SESSION_ID, request(item(TRAINEE_ID, AttendanceStatus.Present, null, null)), STAFF_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Training session not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		verifyNoInteractions(trainingRegistrationRepository, attendanceRecordRepository, auditLogService);
	}

	// --- fixtures -------------------------------------------------------------------------------

	private TrainingSession givenSession(TrainingSessionStatus status) {
		TrainingSession session = session(status);
		when(trainingSessionRepository.findWithClassAndTrainerById(SESSION_ID)).thenReturn(Optional.of(session));
		return session;
	}

	/**
	 * Puts the given trainees on the roster the service asks for, and makes each resolvable by id.
	 * Returns the first trainee.
	 */
	private User givenRegistered(TrainingRegistrationStatus rosterStatus, long... userIds) {
		List<TrainingRegistration> registrations = new ArrayList<>();
		for (long userId : userIds) {
			User user = user(userId, "Trainee " + userId);
			registrations.add(registration(user, rosterStatus));
			when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		}
		when(trainingRegistrationRepository.findByTrainingSessionIdAndStatusInOrderByRegisteredAtAscIdAsc(
				SESSION_ID, List.of(rosterStatus)))
				.thenReturn(registrations);
		return registrations.get(0).getUser();
	}

	private void givenNoExistingRecord(long userId) {
		when(attendanceRecordRepository.findByTrainingSessionIdAndUserId(SESSION_ID, userId))
				.thenReturn(Optional.empty());
	}

	private TrainingSession session(TrainingSessionStatus status) {
		TrainingSession session = new TrainingSession();
		session.setId(SESSION_ID);
		session.setTitle("Concurrency in practice");
		session.setStatus(status);
		return session;
	}

	private TrainingRegistration registration(User user, TrainingRegistrationStatus status) {
		TrainingRegistration registration = new TrainingRegistration();
		registration.setId(user.getId() + 1000L);
		registration.setUser(user);
		registration.setStatus(status);
		return registration;
	}

	private User user(long id, String fullName) {
		User user = new User();
		user.setId(id);
		user.setFullName(fullName);
		user.setEmail("user" + id + "@fap.local");
		return user;
	}

	private AttendanceRecord record(TrainingSession session, User user, AttendanceStatus status) {
		AttendanceRecord record = new AttendanceRecord();
		record.setTrainingSession(session);
		record.setUser(user);
		record.setStatus(status);
		return record;
	}

	private UpdateAttendanceRequest request(AttendanceItemRequest item) {
		return new UpdateAttendanceRequest(List.of(item));
	}

	private AttendanceItemRequest item(
			long userId,
			AttendanceStatus status,
			LocalDateTime checkedInAt,
			AttendanceCheckInMethod checkInMethod) {
		return new AttendanceItemRequest(userId, status, checkedInAt, checkInMethod, null);
	}

	@SuppressWarnings("unchecked")
	private List<AttendanceRecord> captureSavedRecords() {
		ArgumentCaptor<List<AttendanceRecord>> captor = ArgumentCaptor.forClass(List.class);
		verify(attendanceRecordRepository).saveAll(captor.capture());
		return captor.getValue();
	}
}
