package com.fap.result.service;

import com.fap.clazz.entity.ClassEnrollment;
import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassEnrollmentStatus;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.repository.ClassEnrollmentRepository;
import com.fap.clazz.repository.ClassRepository;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.common.i18n.MessageService;
import com.fap.notification.service.NotificationService;
import com.fap.quiz.repository.QuizAssignmentRepository;
import com.fap.quiz.repository.QuizAttemptRepository;
import com.fap.quiz.repository.QuizRepository;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.entity.QuizAttempt;
import com.fap.quiz.enums.QuizAttemptStatus;
import com.fap.result.dto.ClassCourseResultsResponse;
import com.fap.result.dto.CourseResultResponse;
import com.fap.result.dto.UpdateCourseResultRequest;
import com.fap.result.entity.CourseResult;
import com.fap.result.entity.ClassCompletionQuiz;
import com.fap.result.enums.CourseResultStatus;
import com.fap.result.repository.ClassCompletionQuizRepository;
import com.fap.result.repository.CourseResultAdjustmentRepository;
import com.fap.result.repository.CourseResultQuizRepository;
import com.fap.result.repository.CourseResultRepository;
import com.fap.training.entity.AttendanceRecord;
import com.fap.training.entity.TrainingRegistration;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.AttendanceStatus;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.repository.AttendanceRecordRepository;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fap.training.repository.TrainingSessionRepository;
import com.fap.user.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseResultServiceTest {
	private static final Long CLASS_ID = 10L;
	private static final Long USER_ID = 20L;
	private static final Long ACTOR_ID = 30L;

	private final ClassRepository classRepository = mock(ClassRepository.class);
	private final ClassEnrollmentRepository enrollmentRepository = mock(ClassEnrollmentRepository.class);
	private final ClassCompletionQuizRepository completionQuizRepository = mock(ClassCompletionQuizRepository.class);
	private final CourseResultRepository resultRepository = mock(CourseResultRepository.class);
	private final CourseResultQuizRepository resultQuizRepository = mock(CourseResultQuizRepository.class);
	private final CourseResultAdjustmentRepository adjustmentRepository = mock(CourseResultAdjustmentRepository.class);
	private final QuizRepository quizRepository = mock(QuizRepository.class);
	private final QuizAssignmentRepository quizAssignmentRepository = mock(QuizAssignmentRepository.class);
	private final QuizAttemptRepository quizAttemptRepository = mock(QuizAttemptRepository.class);
	private final TrainingSessionRepository sessionRepository = mock(TrainingSessionRepository.class);
	private final TrainingRegistrationRepository registrationRepository = mock(TrainingRegistrationRepository.class);
	private final AttendanceRecordRepository attendanceRepository = mock(AttendanceRecordRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final NotificationService notificationService = mock(NotificationService.class);
	private final MessageService messageService = mock(MessageService.class);

	private final CourseResultService service = new CourseResultService(
			classRepository,
			enrollmentRepository,
			completionQuizRepository,
			resultRepository,
			resultQuizRepository,
			adjustmentRepository,
			quizRepository,
			quizAssignmentRepository,
			quizAttemptRepository,
			sessionRepository,
			registrationRepository,
			attendanceRepository,
			auditLogService,
			notificationService,
			messageService);

	@Test
	void lateAttendanceCountsAsAttendedAndCanPass() {
		FapClass fapClass = givenClass(ClassStatus.Active);
		ClassEnrollment enrollment = givenEnrollment(fapClass, ClassEnrollmentStatus.Enrolled);
		TrainingSession session = new TrainingSession();
		session.setId(100L);
		session.setStatus(TrainingSessionStatus.Completed);
		TrainingRegistration registration = new TrainingRegistration();
		registration.setTrainingSession(session);
		registration.setUser(enrollment.getUser());
		registration.setStatus(TrainingRegistrationStatus.Completed);
		AttendanceRecord attendance = new AttendanceRecord();
		attendance.setTrainingSession(session);
		attendance.setUser(enrollment.getUser());
		attendance.setStatus(AttendanceStatus.Late);
		Quiz quiz = new Quiz();
		quiz.setId(300L);
		quiz.setTitle("Final quiz");
		ClassCompletionQuiz requiredQuiz = new ClassCompletionQuiz();
		requiredQuiz.setFapClass(fapClass);
		requiredQuiz.setQuiz(quiz);
		requiredQuiz.setPassingScore(70);
		QuizAttempt attempt = new QuizAttempt();
		attempt.setId(400L);
		attempt.setQuiz(quiz);
		attempt.setUser(enrollment.getUser());
		attempt.setStatus(QuizAttemptStatus.Submitted);
		attempt.setScore(75);

		AtomicReference<CourseResult> savedResult = new AtomicReference<>();
		when(classRepository.findWithTrainingProgramByIdForUpdate(CLASS_ID)).thenReturn(Optional.of(fapClass));
		when(classRepository.findWithTrainingProgramById(CLASS_ID)).thenReturn(Optional.of(fapClass));
		when(completionQuizRepository.findByFapClassIdOrderByIdAsc(CLASS_ID)).thenReturn(List.of(requiredQuiz));
		when(enrollmentRepository.findByFapClassIdAndStatusInOrderByCreatedAtAscIdAsc(anyLong(), any())).thenReturn(List.of(enrollment));
		when(resultRepository.findByClassEnrollmentId(enrollment.getId())).thenReturn(Optional.empty());
		when(resultRepository.save(any(CourseResult.class))).thenAnswer(invocation -> {
			CourseResult result = invocation.getArgument(0);
			result.setId(500L);
			savedResult.set(result);
			return result;
		});
		when(registrationRepository.findMineByClassId(anyLong(), anyLong(), any())).thenReturn(List.of(registration));
		when(attendanceRepository.findByTrainingSessionFapClassIdAndUserId(CLASS_ID, USER_ID)).thenReturn(List.of(attendance));
		when(quizAttemptRepository.findFirstByQuizIdAndUserIdAndStatusOrderByScoreDescIdDesc(
				quiz.getId(), USER_ID, QuizAttemptStatus.Submitted)).thenReturn(Optional.of(attempt));
		when(resultRepository.findByFapClassIdOrderByClassEnrollmentUserFullNameAsc(CLASS_ID))
				.thenAnswer(invocation -> List.of(savedResult.get()));
		when(resultQuizRepository.findByCourseResultIdOrderByIdAsc(500L)).thenReturn(List.of());
		when(adjustmentRepository.findByCourseResultIdOrderByAdjustedAtDescIdDesc(500L)).thenReturn(List.of());

		ClassCourseResultsResponse response = service.calculate(CLASS_ID, ACTOR_ID);

		CourseResultResponse result = response.results().getFirst();
		assertThat(result.status()).isEqualTo(CourseResultStatus.Passed);
		assertThat(result.attendanceRate()).isEqualByComparingTo("100.00");
		assertThat(result.attendedSessions()).isEqualTo(1);
		assertThat(result.passedQuizCount()).isEqualTo(1);
	}

	@Test
	void classCannotCloseWhileAnySessionIsUpcoming() {
		FapClass fapClass = givenClass(ClassStatus.Active);
		TrainingSession completed = new TrainingSession();
		completed.setStatus(TrainingSessionStatus.Completed);
		TrainingSession upcoming = new TrainingSession();
		upcoming.setStatus(TrainingSessionStatus.Upcoming);
		when(sessionRepository.findByFapClassIdOrderBySessionDateAscStartTimeAsc(CLASS_ID))
				.thenReturn(List.of(completed, upcoming));

		assertThatThrownBy(() -> service.finalizeForClosure(fapClass, ACTOR_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("completed or canceled");
	}

	@Test
	void publishingAgainDoesNotDuplicateNotifications() {
		FapClass fapClass = givenClass(ClassStatus.Closed);
		CourseResult result = givenResult(fapClass, CourseResultStatus.Passed);
		result.setPublishedAt(LocalDateTime.now().minusDays(1));
		when(classRepository.findWithTrainingProgramByIdForUpdate(CLASS_ID)).thenReturn(Optional.of(fapClass));
		when(classRepository.findWithTrainingProgramById(CLASS_ID)).thenReturn(Optional.of(fapClass));
		when(resultRepository.findByFapClassIdOrderByClassEnrollmentUserFullNameAsc(CLASS_ID)).thenReturn(List.of(result));
		when(resultQuizRepository.findByCourseResultIdOrderByIdAsc(result.getId())).thenReturn(List.of());
		when(adjustmentRepository.findByCourseResultIdOrderByAdjustedAtDescIdDesc(result.getId())).thenReturn(List.of());

		service.publish(CLASS_ID, ACTOR_ID);

		verify(notificationService, never()).create(anyLong(), any(), any());
		verify(auditLogService, never()).record("PUBLISH_COURSE_RESULTS", "class", CLASS_ID);
	}

	@Test
	void adjustmentKeepsHistoryAndRequiresRepublishing() {
		FapClass fapClass = givenClass(ClassStatus.Closed);
		CourseResult result = givenResult(fapClass, CourseResultStatus.Failed);
		result.setPublishedAt(LocalDateTime.now());
		when(resultRepository.findForUpdate(CLASS_ID, USER_ID)).thenReturn(Optional.of(result));
		when(resultQuizRepository.findByCourseResultIdOrderByIdAsc(result.getId())).thenReturn(List.of());
		when(adjustmentRepository.findByCourseResultIdOrderByAdjustedAtDescIdDesc(result.getId())).thenReturn(List.of());

		CourseResultResponse response = service.adjust(
				CLASS_ID,
				USER_ID,
				new UpdateCourseResultRequest(CourseResultStatus.Passed, "Approved after review"),
				ACTOR_ID);

		assertThat(response.status()).isEqualTo(CourseResultStatus.Passed);
		assertThat(response.published()).isFalse();
		verify(adjustmentRepository).save(any());
	}

	private FapClass givenClass(ClassStatus status) {
		FapClass fapClass = new FapClass();
		fapClass.setId(CLASS_ID);
		fapClass.setName("Java Fundamentals");
		fapClass.setClassCode("JAVA-01");
		fapClass.setStatus(status);
		fapClass.setMinimumAttendanceRate(BigDecimal.valueOf(80));
		return fapClass;
	}

	private ClassEnrollment givenEnrollment(FapClass fapClass, ClassEnrollmentStatus status) {
		User user = new User();
		user.setId(USER_ID);
		user.setFullName("Trainee One");
		user.setEmail("trainee@example.com");
		ClassEnrollment enrollment = new ClassEnrollment();
		enrollment.setId(200L);
		enrollment.setFapClass(fapClass);
		enrollment.setUser(user);
		enrollment.setStatus(status);
		return enrollment;
	}

	private CourseResult givenResult(FapClass fapClass, CourseResultStatus status) {
		CourseResult result = new CourseResult();
		result.setId(500L);
		result.setFapClass(fapClass);
		result.setClassEnrollment(givenEnrollment(fapClass, ClassEnrollmentStatus.Enrolled));
		result.setCalculatedStatus(status);
		result.setUpdatedAt(LocalDateTime.now());
		return result;
	}
}
