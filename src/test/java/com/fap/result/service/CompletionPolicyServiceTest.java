package com.fap.result.service;

import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.mapper.ClassMapper;
import com.fap.clazz.repository.ClassAdminRepository;
import com.fap.clazz.repository.ClassRepository;
import com.fap.clazz.repository.ClassTrainerRepository;
import com.fap.clazz.service.ClassEnrollmentService;
import com.fap.clazz.service.ClassService;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.program.repository.TrainingProgramRepository;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.enums.QuizStatus;
import com.fap.quiz.repository.QuizAssignmentRepository;
import com.fap.quiz.repository.QuizRepository;
import com.fap.result.dto.CompletionPolicyQuizRequest;
import com.fap.result.dto.CompletionPolicyQuizResponse;
import com.fap.result.dto.CompletionPolicyResponse;
import com.fap.result.dto.UpdateCompletionPolicyRequest;
import com.fap.result.entity.ClassCompletionQuiz;
import com.fap.result.mapper.CourseResultMapper;
import com.fap.result.repository.ClassCompletionQuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The completion policy decides who passes a class, so it is frozen once the class is closed and
 * may only require quizzes assigned directly to that class.
 */
class CompletionPolicyServiceTest {
	private static final Long CLASS_ID = 10L;
	private static final Long ACTOR_ID = 30L;
	private static final Long FIRST_QUIZ_ID = 300L;
	private static final Long SECOND_QUIZ_ID = 301L;

	private final ClassRepository classRepository = mock(ClassRepository.class);
	private final ClassCompletionQuizRepository completionQuizRepository = mock(ClassCompletionQuizRepository.class);
	private final QuizRepository quizRepository = mock(QuizRepository.class);
	private final QuizAssignmentRepository quizAssignmentRepository = mock(QuizAssignmentRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	// A real ClassService: it owns the class write, and the assertions below check the locked class.
	private final ClassService classService = new ClassService(
			classRepository,
			mock(ClassAdminRepository.class),
			mock(ClassTrainerRepository.class),
			mock(TrainingProgramRepository.class),
			mock(ClassMapper.class),
			auditLogService,
			mock(ClassEnrollmentService.class),
			mock(CourseResultService.class));

	private final CompletionPolicyService service = new CompletionPolicyService(
			classRepository,
			classService,
			completionQuizRepository,
			quizRepository,
			quizAssignmentRepository,
			auditLogService,
			new CourseResultMapper());

	@BeforeEach
	void lookupDefaultsDelegateToStubbedFinders() {
		lenient().doCallRealMethod().when(classRepository).getWithTrainingProgramOrThrow(any());
		lenient().doCallRealMethod().when(classRepository).getWithTrainingProgramForUpdateOrThrow(any());
		lenient().doCallRealMethod().when(quizRepository).getQuizOrThrow(any());
	}

	@BeforeEach
	void saveReturnsItsArgument() {
		when(completionQuizRepository.save(any(ClassCompletionQuiz.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void getPolicyReturnsTheMinimumAttendanceAndRequiredQuizzes() {
		FapClass fapClass = givenClass(ClassStatus.Active);
		ClassCompletionQuiz requiredQuiz = new ClassCompletionQuiz();
		requiredQuiz.setFapClass(fapClass);
		requiredQuiz.setQuiz(givenQuiz(FIRST_QUIZ_ID, QuizStatus.Published));
		requiredQuiz.setPassingScore(70);
		when(classRepository.findWithTrainingProgramById(CLASS_ID)).thenReturn(Optional.of(fapClass));
		when(completionQuizRepository.findByFapClassIdOrderByIdAsc(CLASS_ID)).thenReturn(List.of(requiredQuiz));

		CompletionPolicyResponse response = service.getPolicy(CLASS_ID);

		assertThat(response.classId()).isEqualTo(CLASS_ID);
		assertThat(response.minimumAttendanceRate()).isEqualByComparingTo("80");
		assertThat(response.requiredQuizzes()).containsExactly(
				new CompletionPolicyQuizResponse(FIRST_QUIZ_ID, "Quiz 300", 70, QuizStatus.Published));
	}

	@Test
	void getPolicyOfAMissingClassIsNotFound() {
		when(classRepository.findWithTrainingProgramById(CLASS_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getPolicy(CLASS_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Class not found");
	}

	@ParameterizedTest(name = "{0} class policy can be changed")
	@EnumSource(value = ClassStatus.class, names = {"Planning", "Active"})
	void updatePolicyReplacesRequiredQuizzesAndRoundsTheMinimum(ClassStatus status) {
		FapClass fapClass = givenLockedClass(status);
		givenAssignedQuiz(FIRST_QUIZ_ID);
		givenAssignedQuiz(SECOND_QUIZ_ID);

		CompletionPolicyResponse response = service.updatePolicy(CLASS_ID, new UpdateCompletionPolicyRequest(
				new BigDecimal("75.555"),
				List.of(
						new CompletionPolicyQuizRequest(FIRST_QUIZ_ID, 70),
						new CompletionPolicyQuizRequest(SECOND_QUIZ_ID, 50))), ACTOR_ID);

		// The old rows are deleted and flushed before the new ones are inserted.
		InOrder order = inOrder(completionQuizRepository);
		order.verify(completionQuizRepository).deleteByFapClassId(CLASS_ID);
		order.verify(completionQuizRepository).flush();
		ArgumentCaptor<ClassCompletionQuiz> saved = ArgumentCaptor.forClass(ClassCompletionQuiz.class);
		order.verify(completionQuizRepository, times(2)).save(saved.capture());
		assertThat(saved.getAllValues()).allSatisfy(policyQuiz -> {
			assertThat(policyQuiz.getFapClass()).isSameAs(fapClass);
			assertThat(policyQuiz.getCreatedBy()).isEqualTo(ACTOR_ID);
			assertThat(policyQuiz.getUpdatedBy()).isEqualTo(ACTOR_ID);
			assertThat(policyQuiz.getCreatedAt()).isNotNull().isEqualTo(policyQuiz.getUpdatedAt());
		});
		assertThat(saved.getAllValues()).extracting(ClassCompletionQuiz::getPassingScore).containsExactly(70, 50);

		assertThat(fapClass.getMinimumAttendanceRate()).isEqualTo(new BigDecimal("75.56"));
		assertThat(fapClass.getUpdatedBy()).isEqualTo(ACTOR_ID);
		assertThat(fapClass.getUpdatedAt()).isEqualTo(saved.getValue().getUpdatedAt());
		verify(auditLogService).record("UPDATE_COMPLETION_POLICY", "class", CLASS_ID);
		assertThat(response.minimumAttendanceRate()).isEqualTo(new BigDecimal("75.56"));
		assertThat(response.requiredQuizzes()).extracting(CompletionPolicyQuizResponse::quizId)
				.containsExactly(FIRST_QUIZ_ID, SECOND_QUIZ_ID);
	}

	@Test
	void closedClassPolicyIsLocked() {
		FapClass fapClass = givenLockedClass(ClassStatus.Closed);
		givenAssignedQuiz(FIRST_QUIZ_ID);

		assertThatThrownBy(() -> service.updatePolicy(CLASS_ID, new UpdateCompletionPolicyRequest(
				BigDecimal.valueOf(90),
				List.of(new CompletionPolicyQuizRequest(FIRST_QUIZ_ID, 70))), ACTOR_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_COMPLETION_POLICY_LOCKED");

		assertThat(fapClass.getMinimumAttendanceRate()).isEqualByComparingTo("80");
		assertNothingReplaced();
	}

	@Test
	void requiredQuizzesMustBeUnique() {
		givenLockedClass(ClassStatus.Active);
		givenAssignedQuiz(FIRST_QUIZ_ID);

		assertThatThrownBy(() -> service.updatePolicy(CLASS_ID, new UpdateCompletionPolicyRequest(
				BigDecimal.valueOf(80),
				List.of(
						new CompletionPolicyQuizRequest(FIRST_QUIZ_ID, 70),
						new CompletionPolicyQuizRequest(FIRST_QUIZ_ID, 60))), ACTOR_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("DUPLICATE_REQUIRED_QUIZ");

		assertNothingReplaced();
	}

	@Test
	void requiredQuizMustBeAssignedToTheClass() {
		givenLockedClass(ClassStatus.Active);
		when(quizAssignmentRepository.existsByQuizIdAndFapClassId(FIRST_QUIZ_ID, CLASS_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.updatePolicy(CLASS_ID, new UpdateCompletionPolicyRequest(
				BigDecimal.valueOf(80),
				List.of(new CompletionPolicyQuizRequest(FIRST_QUIZ_ID, 70))), ACTOR_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("QUIZ_NOT_ASSIGNED_TO_CLASS");

		assertNothingReplaced();
	}

	@Test
	void updatePolicyOfAMissingClassIsNotFound() {
		when(classRepository.findWithTrainingProgramByIdForUpdate(CLASS_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.updatePolicy(CLASS_ID, new UpdateCompletionPolicyRequest(
				BigDecimal.valueOf(80), List.of()), ACTOR_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Class not found");

		assertNothingReplaced();
	}

	@Test
	void missingRequiredQuizIsNotFound() {
		givenLockedClass(ClassStatus.Active);
		when(quizAssignmentRepository.existsByQuizIdAndFapClassId(FIRST_QUIZ_ID, CLASS_ID)).thenReturn(true);
		when(quizRepository.findById(FIRST_QUIZ_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.updatePolicy(CLASS_ID, new UpdateCompletionPolicyRequest(
				BigDecimal.valueOf(80),
				List.of(new CompletionPolicyQuizRequest(FIRST_QUIZ_ID, 70))), ACTOR_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz not found");

		verify(completionQuizRepository, never()).save(any());
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	private void assertNothingReplaced() {
		verify(completionQuizRepository, never()).deleteByFapClassId(any());
		verify(completionQuizRepository, never()).save(any());
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	private void givenAssignedQuiz(Long quizId) {
		when(quizAssignmentRepository.existsByQuizIdAndFapClassId(quizId, CLASS_ID)).thenReturn(true);
		when(quizRepository.findById(quizId)).thenReturn(Optional.of(givenQuiz(quizId, QuizStatus.Published)));
	}

	private FapClass givenLockedClass(ClassStatus status) {
		FapClass fapClass = givenClass(status);
		when(classRepository.findWithTrainingProgramByIdForUpdate(CLASS_ID)).thenReturn(Optional.of(fapClass));
		return fapClass;
	}

	private static FapClass givenClass(ClassStatus status) {
		FapClass fapClass = new FapClass();
		fapClass.setId(CLASS_ID);
		fapClass.setStatus(status);
		fapClass.setMinimumAttendanceRate(BigDecimal.valueOf(80));
		return fapClass;
	}

	private static Quiz givenQuiz(Long id, QuizStatus status) {
		Quiz quiz = new Quiz();
		quiz.setId(id);
		quiz.setTitle("Quiz " + id);
		quiz.setStatus(status);
		return quiz;
	}
}
