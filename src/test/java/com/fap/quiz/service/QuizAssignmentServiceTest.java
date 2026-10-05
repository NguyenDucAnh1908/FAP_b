package com.fap.quiz.service;

import com.fap.clazz.entity.FapClass;
import com.fap.clazz.repository.ClassRepository;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.quiz.dto.CreateQuizAssignmentRequest;
import com.fap.quiz.dto.QuizAssignmentResponse;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.entity.QuizAssignment;
import com.fap.quiz.enums.QuizStatus;
import com.fap.quiz.mapper.QuizAssignmentMapper;
import com.fap.quiz.repository.QuizAssignmentRepository;
import com.fap.quiz.repository.QuizRepository;
import com.fap.result.repository.ClassCompletionQuizRepository;
import com.fap.training.entity.TrainingSession;
import com.fap.training.repository.TrainingSessionRepository;
import com.fap.user.entity.User;
import com.fap.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A quiz assignment is what makes a quiz reachable for the trainees of one class or one training
 * session, so only a published quiz may gain or lose one, each assignment targets exactly one scope,
 * and an assignment the class completion policy depends on must not disappear from under it.
 */
class QuizAssignmentServiceTest {

	private static final long QUIZ_ID = 31L;
	private static final long ASSIGNMENT_ID = 700L;
	private static final long CLASS_ID = 10L;
	private static final long SESSION_ID = 61L;
	private static final long CURRENT_USER_ID = 7L;

	private final QuizRepository quizRepository = mock(QuizRepository.class);
	private final QuizAssignmentRepository quizAssignmentRepository = mock(QuizAssignmentRepository.class);
	private final ClassRepository classRepository = mock(ClassRepository.class);
	private final TrainingSessionRepository trainingSessionRepository = mock(TrainingSessionRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final ClassCompletionQuizRepository completionQuizRepository = mock(ClassCompletionQuizRepository.class);

	private final QuizAssignmentService service = new QuizAssignmentService(
			quizRepository,
			quizAssignmentRepository,
			classRepository,
			trainingSessionRepository,
			userRepository,
			new QuizAssignmentMapper(),
			auditLogService,
			completionQuizRepository);

	private Quiz quiz;
	private User currentUser;
	private FapClass fapClass;
	private TrainingSession trainingSession;

	@BeforeEach
	void setUp() {
		// The lookup helpers are interface default methods; a plain mock would return null from them.
		lenient().doCallRealMethod().when(quizRepository).getQuizOrThrow(any());
		lenient().doCallRealMethod().when(quizAssignmentRepository).getByQuizIdAndIdOrThrow(any(), any());
		quiz = new Quiz();
		quiz.setId(QUIZ_ID);
		quiz.setTitle("Java basics");
		quiz.setStatus(QuizStatus.Published);
		currentUser = new User();
		currentUser.setId(CURRENT_USER_ID);
		currentUser.setFullName("Class Admin");
		currentUser.setEmail("admin@fap.local");
		fapClass = new FapClass();
		fapClass.setId(CLASS_ID);
		fapClass.setName("Java Fresher 01");
		fapClass.setClassCode("JF01");
		trainingSession = new TrainingSession();
		trainingSession.setId(SESSION_ID);
		trainingSession.setTitle("Spring Boot workshop");
		when(quizRepository.existsById(QUIZ_ID)).thenReturn(true);
		when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
		when(userRepository.getReferenceById(CURRENT_USER_ID)).thenReturn(currentUser);
		when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(fapClass));
		when(trainingSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(trainingSession));
		when(quizAssignmentRepository.save(any(QuizAssignment.class))).thenAnswer(invocation -> {
			QuizAssignment saved = invocation.getArgument(0);
			saved.setId(ASSIGNMENT_ID);
			return saved;
		});
	}

	@Test
	void listRejectsUnknownQuiz() {
		when(quizRepository.existsById(QUIZ_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.list(QUIZ_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz not found")
				.extracting("code")
				.isEqualTo(NotFoundException.CODE);
		verify(quizAssignmentRepository, never()).findByQuizIdOrderByIdAsc(anyLong());
	}

	@Test
	void listMapsAssignmentsInRepositoryOrder() {
		QuizAssignment classAssignment = assignment(701L, fapClass, null);
		QuizAssignment sessionAssignment = assignment(702L, null, trainingSession);
		when(quizAssignmentRepository.findByQuizIdOrderByIdAsc(QUIZ_ID))
				.thenReturn(List.of(classAssignment, sessionAssignment));

		List<QuizAssignmentResponse> responses = service.list(QUIZ_ID);

		assertThat(responses).extracting(QuizAssignmentResponse::id).containsExactly(701L, 702L);
		assertThat(responses.get(0).classId()).isEqualTo(CLASS_ID);
		assertThat(responses.get(0).classCode()).isEqualTo("JF01");
		assertThat(responses.get(0).trainingSessionId()).isNull();
		assertThat(responses.get(1).classId()).isNull();
		assertThat(responses.get(1).trainingSessionId()).isEqualTo(SESSION_ID);
		assertThat(responses.get(1).trainingSessionTitle()).isEqualTo("Spring Boot workshop");
	}

	@Test
	void assignToClassCreatesAClassScopedAssignment() {
		LocalDateTime before = LocalDateTime.now();

		QuizAssignmentResponse response = service.assign(
				QUIZ_ID, new CreateQuizAssignmentRequest(CLASS_ID, null), CURRENT_USER_ID);

		LocalDateTime after = LocalDateTime.now();
		QuizAssignment saved = captureSaved();
		assertThat(saved.getQuiz()).isSameAs(quiz);
		assertThat(saved.getFapClass()).isSameAs(fapClass);
		assertThat(saved.getTrainingSession()).isNull();
		assertThat(saved.getAssignedBy()).isSameAs(currentUser);
		assertThat(saved.getAssignedAt()).isBetween(before, after);
		assertThat(response.id()).isEqualTo(ASSIGNMENT_ID);
		assertThat(response.quizId()).isEqualTo(QUIZ_ID);
		assertThat(response.classId()).isEqualTo(CLASS_ID);
		assertThat(response.className()).isEqualTo("Java Fresher 01");
		assertThat(response.assignedBy()).isEqualTo(CURRENT_USER_ID);
		verify(auditLogService).record("CREATE_QUIZ_ASSIGNMENT", "quiz_assignment", ASSIGNMENT_ID);
		verifyNoInteractions(trainingSessionRepository);
		verify(quizAssignmentRepository, never()).existsByQuizIdAndTrainingSessionId(any(), any());
	}

	@Test
	void assignToSessionCreatesASessionScopedAssignment() {
		QuizAssignmentResponse response = service.assign(
				QUIZ_ID, new CreateQuizAssignmentRequest(null, SESSION_ID), CURRENT_USER_ID);

		QuizAssignment saved = captureSaved();
		assertThat(saved.getTrainingSession()).isSameAs(trainingSession);
		assertThat(saved.getFapClass()).isNull();
		assertThat(saved.getAssignedBy()).isSameAs(currentUser);
		assertThat(saved.getAssignedAt()).isNotNull();
		assertThat(response.trainingSessionId()).isEqualTo(SESSION_ID);
		assertThat(response.trainingSessionTitle()).isEqualTo("Spring Boot workshop");
		assertThat(response.classId()).isNull();
		verify(auditLogService).record("CREATE_QUIZ_ASSIGNMENT", "quiz_assignment", ASSIGNMENT_ID);
		verifyNoInteractions(classRepository);
		verify(quizAssignmentRepository, never()).existsByQuizIdAndFapClassId(any(), any());
	}

	@Test
	void assignRejectsUnknownQuiz() {
		when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.assign(
				QUIZ_ID, new CreateQuizAssignmentRequest(CLASS_ID, null), CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz not found");
		verifyNothingSaved();
	}

	@ParameterizedTest(name = "a {0} quiz cannot be assigned")
	@EnumSource(value = QuizStatus.class, names = {"Draft", "Closed"})
	void assignRejectsQuizThatIsNotPublished(QuizStatus status) {
		quiz.setStatus(status);

		assertThatThrownBy(() -> service.assign(
				QUIZ_ID, new CreateQuizAssignmentRequest(CLASS_ID, null), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Only published quiz can be assigned")
				.extracting("code")
				.isEqualTo("QUIZ_NOT_ASSIGNABLE");
		verifyNothingSaved();
	}

	@Test
	void assignRejectsBothScopesAtOnce() {
		assertThatThrownBy(() -> service.assign(
				QUIZ_ID, new CreateQuizAssignmentRequest(CLASS_ID, SESSION_ID), CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Provide exactly one of classId or trainingSessionId")
				.extracting("code")
				.isEqualTo("INVALID_QUIZ_ASSIGNMENT_SCOPE");
		verifyNoInteractions(classRepository, trainingSessionRepository);
		verifyNothingSaved();
	}

	@Test
	void assignRejectsMissingScope() {
		assertThatThrownBy(() -> service.assign(
				QUIZ_ID, new CreateQuizAssignmentRequest(null, null), CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_QUIZ_ASSIGNMENT_SCOPE");
		verifyNothingSaved();
	}

	@Test
	void assignChecksThePublishedStatusBeforeTheScope() {
		quiz.setStatus(QuizStatus.Draft);

		assertThatThrownBy(() -> service.assign(
				QUIZ_ID, new CreateQuizAssignmentRequest(CLASS_ID, SESSION_ID), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("QUIZ_NOT_ASSIGNABLE");
	}

	@Test
	void assignRejectsDuplicateClassAssignmentBeforeLoadingTheClass() {
		when(quizAssignmentRepository.existsByQuizIdAndFapClassId(QUIZ_ID, CLASS_ID)).thenReturn(true);

		assertThatThrownBy(() -> service.assign(
				QUIZ_ID, new CreateQuizAssignmentRequest(CLASS_ID, null), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Quiz is already assigned to this class")
				.extracting("code")
				.isEqualTo("DUPLICATE_QUIZ_CLASS_ASSIGNMENT");
		verify(classRepository, never()).findById(any());
		verifyNothingSaved();
	}

	@Test
	void assignRejectsDuplicateSessionAssignmentBeforeLoadingTheSession() {
		when(quizAssignmentRepository.existsByQuizIdAndTrainingSessionId(QUIZ_ID, SESSION_ID)).thenReturn(true);

		assertThatThrownBy(() -> service.assign(
				QUIZ_ID, new CreateQuizAssignmentRequest(null, SESSION_ID), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Quiz is already assigned to this training session")
				.extracting("code")
				.isEqualTo("DUPLICATE_QUIZ_SESSION_ASSIGNMENT");
		verify(trainingSessionRepository, never()).findById(any());
		verifyNothingSaved();
	}

	/** Duplicates are per scope: a class assignment does not block assigning one of its sessions. */
	@Test
	void assignToSessionIgnoresAnExistingClassAssignment() {
		when(quizAssignmentRepository.existsByQuizIdAndFapClassId(QUIZ_ID, CLASS_ID)).thenReturn(true);

		service.assign(QUIZ_ID, new CreateQuizAssignmentRequest(null, SESSION_ID), CURRENT_USER_ID);

		assertThat(captureSaved().getTrainingSession()).isSameAs(trainingSession);
	}

	@Test
	void assignRejectsUnknownClass() {
		when(classRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.assign(
				QUIZ_ID, new CreateQuizAssignmentRequest(CLASS_ID, null), CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Class not found")
				.extracting("code")
				.isEqualTo(NotFoundException.CODE);
		verifyNothingSaved();
	}

	@Test
	void assignRejectsUnknownSession() {
		when(trainingSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.assign(
				QUIZ_ID, new CreateQuizAssignmentRequest(null, SESSION_ID), CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Training session not found")
				.extracting("code")
				.isEqualTo(NotFoundException.CODE);
		verifyNothingSaved();
	}

	@Test
	void deleteRemovesASessionAssignmentWithoutConsultingTheCompletionPolicy() {
		QuizAssignment assignment = givenAssignment(null, trainingSession);

		service.delete(QUIZ_ID, ASSIGNMENT_ID);

		InOrder order = inOrder(quizAssignmentRepository, auditLogService);
		order.verify(quizAssignmentRepository).delete(assignment);
		order.verify(auditLogService).record("DELETE_QUIZ_ASSIGNMENT", "quiz_assignment", ASSIGNMENT_ID);
		verifyNoInteractions(completionQuizRepository);
	}

	@Test
	void deleteRemovesAClassAssignmentTheCompletionPolicyDoesNotNeed() {
		QuizAssignment assignment = givenAssignment(fapClass, null);
		when(completionQuizRepository.existsByFapClassIdAndQuizId(CLASS_ID, QUIZ_ID)).thenReturn(false);

		service.delete(QUIZ_ID, ASSIGNMENT_ID);

		verify(completionQuizRepository).existsByFapClassIdAndQuizId(CLASS_ID, QUIZ_ID);
		InOrder order = inOrder(quizAssignmentRepository, auditLogService);
		order.verify(quizAssignmentRepository).delete(assignment);
		order.verify(auditLogService).record("DELETE_QUIZ_ASSIGNMENT", "quiz_assignment", ASSIGNMENT_ID);
	}

	@Test
	void deleteRejectsAClassAssignmentRequiredByTheCompletionPolicy() {
		givenAssignment(fapClass, null);
		when(completionQuizRepository.existsByFapClassIdAndQuizId(CLASS_ID, QUIZ_ID)).thenReturn(true);

		assertThatThrownBy(() -> service.delete(QUIZ_ID, ASSIGNMENT_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Quiz is required by the class completion policy")
				.extracting("code")
				.isEqualTo("QUIZ_REQUIRED_FOR_COMPLETION");
		verifyNothingDeleted();
	}

	/** Scoped to the quiz, so an assignment id belonging to another quiz reads as not found. */
	@Test
	void deleteRejectsAnUnknownOrForeignAssignment() {
		when(quizAssignmentRepository.findByQuizIdAndId(QUIZ_ID, ASSIGNMENT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(QUIZ_ID, ASSIGNMENT_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz assignment not found")
				.extracting("code")
				.isEqualTo(NotFoundException.CODE);
		verifyNothingDeleted();
	}

	/**
	 * Pins current behaviour: once a quiz is closed its assignments are frozen too, and the error
	 * reuses the assign-time code and message ("Only published quiz can be assigned").
	 */
	@ParameterizedTest(name = "an assignment of a {0} quiz cannot be removed")
	@EnumSource(value = QuizStatus.class, names = {"Draft", "Closed"})
	void deleteRejectsAnAssignmentOfAQuizThatIsNotPublished(QuizStatus status) {
		quiz.setStatus(status);
		givenAssignment(fapClass, null);

		assertThatThrownBy(() -> service.delete(QUIZ_ID, ASSIGNMENT_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Only published quiz can be assigned")
				.extracting("code")
				.isEqualTo("QUIZ_NOT_ASSIGNABLE");
		verifyNoInteractions(completionQuizRepository);
		verifyNothingDeleted();
	}

	private QuizAssignment givenAssignment(FapClass assignedClass, TrainingSession assignedSession) {
		QuizAssignment assignment = assignment(ASSIGNMENT_ID, assignedClass, assignedSession);
		when(quizAssignmentRepository.findByQuizIdAndId(QUIZ_ID, ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
		return assignment;
	}

	private QuizAssignment assignment(long id, FapClass assignedClass, TrainingSession assignedSession) {
		QuizAssignment assignment = new QuizAssignment();
		assignment.setId(id);
		assignment.setQuiz(quiz);
		assignment.setFapClass(assignedClass);
		assignment.setTrainingSession(assignedSession);
		assignment.setAssignedBy(currentUser);
		assignment.setAssignedAt(LocalDateTime.of(2026, 3, 1, 8, 0));
		return assignment;
	}

	private QuizAssignment captureSaved() {
		ArgumentCaptor<QuizAssignment> captor = ArgumentCaptor.forClass(QuizAssignment.class);
		verify(quizAssignmentRepository).save(captor.capture());
		return captor.getValue();
	}

	private void verifyNothingSaved() {
		verify(quizAssignmentRepository, never()).save(any());
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	private void verifyNothingDeleted() {
		verify(quizAssignmentRepository, never()).delete(any());
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}
}
