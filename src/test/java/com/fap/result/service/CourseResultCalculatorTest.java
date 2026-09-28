package com.fap.result.service;

import com.fap.clazz.entity.ClassEnrollment;
import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassEnrollmentStatus;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.repository.ClassEnrollmentRepository;
import com.fap.quiz.repository.QuizAttemptRepository;
import com.fap.result.entity.CourseResult;
import com.fap.result.enums.CourseResultStatus;
import com.fap.result.repository.ClassCompletionQuizRepository;
import com.fap.result.repository.CourseResultQuizRepository;
import com.fap.result.repository.CourseResultRepository;
import com.fap.training.entity.AttendanceRecord;
import com.fap.training.entity.TrainingRegistration;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.AttendanceStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.repository.AttendanceRecordRepository;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fap.user.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins how a trainee's outcome is derived: attendance counts only completed sessions, a class with
 * no completed session cannot be passed, and recalculation keeps manual overrides of active
 * trainees while a withdrawal drops them.
 */
class CourseResultCalculatorTest {
	private static final Long CLASS_ID = 10L;
	private static final Long USER_ID = 20L;
	private static final Long ACTOR_ID = 30L;

	private final ClassEnrollmentRepository enrollmentRepository = mock(ClassEnrollmentRepository.class);
	private final ClassCompletionQuizRepository completionQuizRepository = mock(ClassCompletionQuizRepository.class);
	private final CourseResultRepository resultRepository = mock(CourseResultRepository.class);
	private final CourseResultQuizRepository resultQuizRepository = mock(CourseResultQuizRepository.class);
	private final QuizAttemptRepository quizAttemptRepository = mock(QuizAttemptRepository.class);
	private final TrainingRegistrationRepository registrationRepository = mock(TrainingRegistrationRepository.class);
	private final AttendanceRecordRepository attendanceRepository = mock(AttendanceRecordRepository.class);

	private final CourseResultCalculator calculator = new CourseResultCalculator(
			enrollmentRepository,
			completionQuizRepository,
			resultRepository,
			resultQuizRepository,
			quizAttemptRepository,
			registrationRepository,
			attendanceRepository);

	private final FapClass fapClass = givenClass();

	@Test
	void attendanceAtTheMinimumPassesAndOnlyCompletedSessionsCount() {
		ClassEnrollment enrollment = givenEnrollment(200L, USER_ID, ClassEnrollmentStatus.Enrolled);
		CourseResult result = givenResult(enrollment);
		List<TrainingRegistration> registrations = new ArrayList<>();
		List<AttendanceRecord> attendance = new ArrayList<>();
		for (long sessionId = 1; sessionId <= 5; sessionId++) {
			TrainingSession session = givenSession(sessionId, TrainingSessionStatus.Completed);
			registrations.add(givenRegistration(session, enrollment.getUser()));
			if (sessionId <= 4) {
				attendance.add(givenAttendance(session, enrollment.getUser(), AttendanceStatus.Present));
			}
		}
		// Attended but still upcoming, so it counts neither as attended nor as held.
		TrainingSession upcoming = givenSession(6L, TrainingSessionStatus.Upcoming);
		registrations.add(givenRegistration(upcoming, enrollment.getUser()));
		attendance.add(givenAttendance(upcoming, enrollment.getUser(), AttendanceStatus.Present));
		givenClassData(List.of(enrollment), List.of(result), registrations, attendance);

		List<CourseResult> results = calculator.calculateAll(fapClass, ACTOR_ID);

		assertThat(results).containsExactly(result);
		assertThat(result.getAttendedSessions()).isEqualTo(4);
		assertThat(result.getTotalSessions()).isEqualTo(5);
		assertThat(result.getAttendanceRate()).isEqualTo(new BigDecimal("80.00"));
		assertThat(result.getCalculatedStatus()).isEqualTo(CourseResultStatus.Passed);
		assertThat(result.getCalculatedBy()).isEqualTo(ACTOR_ID);
		verify(quizAttemptRepository, never()).findSubmittedForClassOrderByScoreDesc(anyLong(), any());
	}

	@Test
	void missingResultIsCreatedAndFailsWithoutAnyCompletedSession() {
		ClassEnrollment enrollment = givenEnrollment(200L, USER_ID, ClassEnrollmentStatus.Enrolled);
		givenClassData(List.of(enrollment), List.of(), List.of(), List.of());
		when(resultRepository.save(any(CourseResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

		List<CourseResult> results = calculator.calculateAll(fapClass, ACTOR_ID);

		assertThat(results).singleElement().satisfies(result -> {
			assertThat(result.getFapClass()).isSameAs(fapClass);
			assertThat(result.getClassEnrollment()).isSameAs(enrollment);
			assertThat(result.getTotalSessions()).isZero();
			assertThat(result.getAttendanceRate()).isEqualTo(new BigDecimal("0.00"));
			assertThat(result.getCalculatedStatus()).isEqualTo(CourseResultStatus.Failed);
		});
		verify(resultRepository).save(any(CourseResult.class));
		verify(resultQuizRepository).saveAll(List.of());
	}

	@Test
	void recalculationKeepsTheOverrideButRequiresRepublishing() {
		ClassEnrollment enrollment = givenEnrollment(200L, USER_ID, ClassEnrollmentStatus.Completed);
		CourseResult result = givenResult(enrollment);
		result.setOverrideStatus(CourseResultStatus.Passed);
		result.setPublishedAt(LocalDateTime.now().minusDays(1));
		result.setPublishedBy(ACTOR_ID);
		givenClassData(List.of(enrollment), List.of(result), List.of(), List.of());

		calculator.calculateAll(fapClass, ACTOR_ID);

		assertThat(result.getCalculatedStatus()).isEqualTo(CourseResultStatus.Failed);
		assertThat(result.getOverrideStatus()).isEqualTo(CourseResultStatus.Passed);
		assertThat(result.getPublishedAt()).isNull();
		assertThat(result.getPublishedBy()).isNull();
	}

	@Test
	void withdrawalDropsOnlyTheOverrideStatus() {
		ClassEnrollment enrollment = givenEnrollment(201L, 21L, ClassEnrollmentStatus.Withdrawn);
		CourseResult result = givenResult(enrollment);
		result.setCalculatedStatus(CourseResultStatus.Failed);
		result.setOverrideStatus(CourseResultStatus.Passed);
		result.setOverrideReason("Approved after review");
		LocalDateTime publishedAt = LocalDateTime.now().minusDays(1);
		result.setPublishedAt(publishedAt);
		givenClassData(List.of(enrollment), List.of(result), List.of(), List.of());

		calculator.calculateAll(fapClass, ACTOR_ID);

		assertThat(result.getCalculatedStatus()).isEqualTo(CourseResultStatus.Withdrawn);
		assertThat(result.getOverrideStatus()).isNull();
		assertThat(result.getOverrideReason()).isEqualTo("Approved after review");
		assertThat(result.getPublishedAt()).isEqualTo(publishedAt);
		assertThat(result.getCalculatedBy()).isEqualTo(ACTOR_ID);
		verify(resultQuizRepository).saveAll(List.of());
	}

	private void givenClassData(
			List<ClassEnrollment> enrollments,
			List<CourseResult> results,
			List<TrainingRegistration> registrations,
			List<AttendanceRecord> attendance) {
		when(completionQuizRepository.findByFapClassIdOrderByIdAsc(CLASS_ID)).thenReturn(List.of());
		when(enrollmentRepository.findByFapClassIdAndStatusInOrderByCreatedAtAscIdAsc(eq(CLASS_ID), any()))
				.thenReturn(enrollments);
		when(resultRepository.findByFapClassId(CLASS_ID)).thenReturn(results);
		when(registrationRepository.findByClassIdAndStatusIn(eq(CLASS_ID), any())).thenReturn(registrations);
		when(attendanceRepository.findByTrainingSessionFapClassId(CLASS_ID)).thenReturn(attendance);
	}

	private static FapClass givenClass() {
		FapClass fapClass = new FapClass();
		fapClass.setId(CLASS_ID);
		fapClass.setStatus(ClassStatus.Active);
		fapClass.setMinimumAttendanceRate(new BigDecimal("80.00"));
		return fapClass;
	}

	private ClassEnrollment givenEnrollment(Long id, Long userId, ClassEnrollmentStatus status) {
		User user = new User();
		user.setId(userId);
		ClassEnrollment enrollment = new ClassEnrollment();
		enrollment.setId(id);
		enrollment.setFapClass(fapClass);
		enrollment.setUser(user);
		enrollment.setStatus(status);
		return enrollment;
	}

	private CourseResult givenResult(ClassEnrollment enrollment) {
		CourseResult result = new CourseResult();
		result.setId(enrollment.getId() + 300);
		result.setFapClass(fapClass);
		result.setClassEnrollment(enrollment);
		result.setUpdatedAt(LocalDateTime.now());
		return result;
	}

	private static TrainingSession givenSession(Long id, TrainingSessionStatus status) {
		TrainingSession session = new TrainingSession();
		session.setId(id);
		session.setStatus(status);
		return session;
	}

	private static TrainingRegistration givenRegistration(TrainingSession session, User user) {
		TrainingRegistration registration = new TrainingRegistration();
		registration.setTrainingSession(session);
		registration.setUser(user);
		return registration;
	}

	private static AttendanceRecord givenAttendance(TrainingSession session, User user, AttendanceStatus status) {
		AttendanceRecord attendance = new AttendanceRecord();
		attendance.setTrainingSession(session);
		attendance.setUser(user);
		attendance.setStatus(status);
		return attendance;
	}
}
