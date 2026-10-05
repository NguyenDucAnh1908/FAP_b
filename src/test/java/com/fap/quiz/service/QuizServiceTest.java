package com.fap.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.quiz.dto.CreateQuizRequest;
import com.fap.quiz.dto.QuizQuestionItemRequest;
import com.fap.quiz.dto.QuizQuestionResponse;
import com.fap.quiz.dto.QuizResponse;
import com.fap.quiz.dto.UpdateQuizQuestionsRequest;
import com.fap.quiz.dto.UpdateQuizRequest;
import com.fap.quiz.entity.Question;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.entity.QuizQuestion;
import com.fap.quiz.entity.QuizQuestionId;
import com.fap.quiz.enums.QuestionType;
import com.fap.quiz.enums.QuizStatus;
import com.fap.quiz.mapper.QuestionMapper;
import com.fap.quiz.mapper.QuizMapper;
import com.fap.quiz.repository.QuestionRepository;
import com.fap.quiz.repository.QuizQuestionRepository;
import com.fap.quiz.repository.QuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Authoring rules for a quiz: only a Draft quiz may be edited, deleted or have its question set
 * replaced, because a Published quiz may already have attempts graded against those questions. The
 * lifecycle transitions themselves are covered by {@link QuizStateMachineTest}.
 */
class QuizServiceTest {

	private static final long QUIZ_ID = 31L;
	private static final long FIRST_QUESTION_ID = 501L;
	private static final long SECOND_QUESTION_ID = 502L;
	private static final long AUTHOR_ID = 1L;
	private static final long CURRENT_USER_ID = 7L;
	private static final LocalDate OPEN_DATE = LocalDate.of(2026, 4, 1);
	private static final LocalDate CLOSE_DATE = LocalDate.of(2026, 4, 30);
	private static final LocalDateTime CREATED_AT = LocalDateTime.of(2020, 1, 5, 8, 0);

	private final QuizRepository quizRepository = mock(QuizRepository.class);
	private final QuizQuestionRepository quizQuestionRepository = mock(QuizQuestionRepository.class);
	private final QuestionRepository questionRepository = mock(QuestionRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);

	private final QuizService service = new QuizService(
			quizRepository,
			quizQuestionRepository,
			questionRepository,
			new QuizMapper(new QuestionMapper(new ObjectMapper())),
			auditLogService);

	@BeforeEach
	void setUp() {
		lenient().doCallRealMethod().when(quizRepository).getQuizOrThrow(any());
		when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> {
			Quiz saved = invocation.getArgument(0);
			saved.setId(QUIZ_ID);
			return saved;
		});
	}

	@Test
	void createStartsAsDraftWithTrimmedFieldsAndAudits() {
		when(quizQuestionRepository.countByIdQuizId(QUIZ_ID)).thenReturn(0L);

		QuizResponse response = service.create(new CreateQuizRequest(
				"  Java basics  ", "   ", 30, 70, 2, true, "  java  ", OPEN_DATE, CLOSE_DATE), CURRENT_USER_ID);

		Quiz saved = captureSaved();
		assertThat(saved.getStatus()).isEqualTo(QuizStatus.Draft);
		assertThat(saved.getTitle()).isEqualTo("Java basics");
		assertThat(saved.getDescription()).isNull();
		assertThat(saved.getCategory()).isEqualTo("java");
		assertThat(saved.getDurationMinutes()).isEqualTo(30);
		assertThat(saved.getPassingScore()).isEqualTo(70);
		assertThat(saved.getMaxAttempts()).isEqualTo(2);
		assertThat(saved.isRandomize()).isTrue();
		assertThat(saved.getOpenDate()).isEqualTo(OPEN_DATE);
		assertThat(saved.getCloseDate()).isEqualTo(CLOSE_DATE);
		assertThat(saved.isDeleted()).isFalse();
		assertThat(saved.getCreatedBy()).isEqualTo(CURRENT_USER_ID);
		assertThat(saved.getUpdatedBy()).isEqualTo(CURRENT_USER_ID);
		assertThat(saved.getCreatedAt()).isNotNull().isEqualTo(saved.getUpdatedAt());
		verify(auditLogService).record("CREATE_QUIZ", "quiz", QUIZ_ID);
		assertThat(response.id()).isEqualTo(QUIZ_ID);
		assertThat(response.status()).isEqualTo(QuizStatus.Draft);
		assertThat(response.questionCount()).isZero();
	}

	@Test
	void createTrimsAPresentDescription() {
		service.create(createRequest("  Covers the JVM  ", OPEN_DATE, CLOSE_DATE), CURRENT_USER_ID);

		assertThat(captureSaved().getDescription()).isEqualTo("Covers the JVM");
	}

	@ParameterizedTest(name = "open {0}, close {1} is a valid window")
	@CsvSource({
			"2026-04-01, 2026-04-30",
			"2026-04-01, 2026-04-01",
			"2026-04-01, ",
			", 2026-04-30",
			", "
	})
	void createAcceptsAnInclusiveOrOpenEndedWindow(LocalDate openDate, LocalDate closeDate) {
		service.create(createRequest(null, openDate, closeDate), CURRENT_USER_ID);

		Quiz saved = captureSaved();
		assertThat(saved.getOpenDate()).isEqualTo(openDate);
		assertThat(saved.getCloseDate()).isEqualTo(closeDate);
	}

	@Test
	void createRejectsOpenDateAfterCloseDate() {
		CreateQuizRequest request = createRequest(null, CLOSE_DATE.plusDays(1), CLOSE_DATE);

		assertThatThrownBy(() -> service.create(request, CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Quiz open date must be before or equal to close date")
				.extracting("code")
				.isEqualTo("INVALID_QUIZ_DATE_RANGE");

		verify(quizRepository, never()).save(any());
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@Test
	void updateAppliesFieldsToDraftQuizKeepsAuthorAndAudits() {
		Quiz quiz = givenQuiz(QuizStatus.Draft);
		when(quizQuestionRepository.countByIdQuizId(QUIZ_ID)).thenReturn(4L);

		QuizResponse response = service.update(QUIZ_ID, new UpdateQuizRequest(
				"  Advanced Java  ", "  Generics  ", 45, 80, 3, false, "  java-advanced  ", OPEN_DATE, CLOSE_DATE),
				CURRENT_USER_ID);

		assertThat(quiz.getTitle()).isEqualTo("Advanced Java");
		assertThat(quiz.getDescription()).isEqualTo("Generics");
		assertThat(quiz.getDurationMinutes()).isEqualTo(45);
		assertThat(quiz.getPassingScore()).isEqualTo(80);
		assertThat(quiz.getMaxAttempts()).isEqualTo(3);
		assertThat(quiz.isRandomize()).isFalse();
		assertThat(quiz.getCategory()).isEqualTo("java-advanced");
		assertThat(quiz.getOpenDate()).isEqualTo(OPEN_DATE);
		assertThat(quiz.getCloseDate()).isEqualTo(CLOSE_DATE);
		assertThat(quiz.getStatus()).isEqualTo(QuizStatus.Draft);
		assertThat(quiz.getUpdatedBy()).isEqualTo(CURRENT_USER_ID);
		assertThat(quiz.getUpdatedAt()).isAfter(CREATED_AT);
		assertThat(quiz.getCreatedBy()).isEqualTo(AUTHOR_ID);
		assertThat(quiz.getCreatedAt()).isEqualTo(CREATED_AT);
		verify(auditLogService).record("UPDATE_QUIZ", "quiz", QUIZ_ID);
		assertThat(response.questionCount()).isEqualTo(4);
	}

	@ParameterizedTest(name = "a {0} quiz cannot be updated")
	@EnumSource(value = QuizStatus.class, names = {"Published", "Closed"})
	void updateRejectsNonDraftQuiz(QuizStatus status) {
		Quiz quiz = givenQuiz(status);

		assertThatThrownBy(() -> service.update(QUIZ_ID, updateRequest(OPEN_DATE, CLOSE_DATE), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Only draft quiz can be edited")
				.extracting("code")
				.isEqualTo("QUIZ_NOT_EDITABLE");

		assertThat(quiz.getTitle()).isEqualTo("Original title");
		assertThat(quiz.getUpdatedBy()).isEqualTo(AUTHOR_ID);
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@Test
	void updateReportsNonDraftBeforeAnInvalidDateRange() {
		givenQuiz(QuizStatus.Published);

		assertThatThrownBy(() -> service.update(
				QUIZ_ID, updateRequest(CLOSE_DATE.plusDays(1), CLOSE_DATE), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("QUIZ_NOT_EDITABLE");
	}

	@Test
	void updateRejectsOpenDateAfterCloseDateAndLeavesTheQuizUntouched() {
		Quiz quiz = givenQuiz(QuizStatus.Draft);

		assertThatThrownBy(() -> service.update(
				QUIZ_ID, updateRequest(CLOSE_DATE.plusDays(1), CLOSE_DATE), CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_QUIZ_DATE_RANGE");

		assertThat(quiz.getTitle()).isEqualTo("Original title");
		assertThat(quiz.getOpenDate()).isNull();
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@Test
	void updateRejectsUnknownQuiz() {
		when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(QUIZ_ID, updateRequest(OPEN_DATE, CLOSE_DATE), CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");
	}

	@Test
	void deleteSoftDeletesDraftQuizAndAudits() {
		Quiz quiz = givenQuiz(QuizStatus.Draft);

		service.delete(QUIZ_ID, CURRENT_USER_ID);

		assertThat(quiz.isDeleted()).isTrue();
		assertThat(quiz.getDeletedAt()).isNotNull().isEqualTo(quiz.getUpdatedAt());
		assertThat(quiz.getUpdatedBy()).isEqualTo(CURRENT_USER_ID);
		assertThat(quiz.getStatus()).isEqualTo(QuizStatus.Draft);
		verify(quizRepository, never()).delete(any());
		verify(quizRepository, never()).deleteById(any());
		verify(auditLogService).record("DELETE_QUIZ", "quiz", QUIZ_ID);
	}

	@ParameterizedTest(name = "a {0} quiz cannot be deleted")
	@EnumSource(value = QuizStatus.class, names = {"Published", "Closed"})
	void deleteRejectsNonDraftQuiz(QuizStatus status) {
		Quiz quiz = givenQuiz(status);

		assertThatThrownBy(() -> service.delete(QUIZ_ID, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("QUIZ_NOT_EDITABLE");

		assertThat(quiz.isDeleted()).isFalse();
		assertThat(quiz.getDeletedAt()).isNull();
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@Test
	void deleteRejectsUnknownQuiz() {
		when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(QUIZ_ID, CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz not found");

		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@Test
	void getReturnsTheQuizWithItsQuestionCount() {
		givenQuiz(QuizStatus.Published);
		when(quizQuestionRepository.countByIdQuizId(QUIZ_ID)).thenReturn(5L);

		QuizResponse response = service.get(QUIZ_ID);

		assertThat(response.id()).isEqualTo(QUIZ_ID);
		assertThat(response.status()).isEqualTo(QuizStatus.Published);
		assertThat(response.questionCount()).isEqualTo(5);
	}

	@Test
	void getRejectsUnknownQuiz() {
		when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(QUIZ_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz not found");
	}

	@Test
	void listNormalizesFiltersAndMapsSortFieldToColumn() {
		Quiz quiz = givenQuiz(QuizStatus.Published);
		when(quizRepository.search(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(quiz)));

		List<QuizResponse> content = service.list(
				QuizStatus.Published, "   ", "  java  ", 0, 20, "closeDate", "asc").getContent();

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(quizRepository).search(eq("Published"), isNull(), eq("java"), pageable.capture());
		assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "close_date"));
		assertThat(content).extracting(QuizResponse::id).containsExactly(QUIZ_ID);
	}

	@Test
	void listDefaultsToNewestFirst() {
		when(quizRepository.search(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

		service.list(null, null, null, 0, 20);

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(quizRepository).search(isNull(), isNull(), isNull(), pageable.capture());
		assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "id"));
	}

	@Test
	void listRejectsUnknownSortField() {
		assertThatThrownBy(() -> service.list(null, null, null, 0, 20, "passingScore", "asc"))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_SORT_FIELD");

		verify(quizRepository, never()).search(any(), any(), any(), any());
	}

	@Test
	void listQuestionsReturnsThemInRepositoryOrder() {
		Quiz quiz = givenQuiz(QuizStatus.Published);
		when(quizRepository.existsById(QUIZ_ID)).thenReturn(true);
		when(quizQuestionRepository.findByIdQuizIdOrderBySortOrderAsc(QUIZ_ID)).thenReturn(List.of(
				quizQuestion(quiz, question(SECOND_QUESTION_ID), 1, new BigDecimal("2.00")),
				quizQuestion(quiz, question(FIRST_QUESTION_ID), 2, BigDecimal.ONE)));

		List<QuizQuestionResponse> questions = service.listQuestions(QUIZ_ID);

		assertThat(questions).extracting(QuizQuestionResponse::questionId)
				.containsExactly(SECOND_QUESTION_ID, FIRST_QUESTION_ID);
		assertThat(questions).extracting(QuizQuestionResponse::sortOrder).containsExactly(1, 2);
		assertThat(questions.get(0).points()).isEqualByComparingTo("2");
		assertThat(questions.get(0).question().content()).isEqualTo("Question " + SECOND_QUESTION_ID);
	}

	@Test
	void listQuestionsRejectsUnknownQuiz() {
		when(quizRepository.existsById(QUIZ_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.listQuestions(QUIZ_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz not found");

		verify(quizQuestionRepository, never()).findByIdQuizIdOrderBySortOrderAsc(anyLong());
	}

	@Test
	void replaceQuestionsSwapsTheWholeSetAndReturnsItBySortOrder() {
		Quiz quiz = givenQuiz(QuizStatus.Draft);
		Question first = question(FIRST_QUESTION_ID);
		Question second = question(SECOND_QUESTION_ID);
		when(questionRepository.findAllById(anyIterable())).thenReturn(List.of(first, second));

		List<QuizQuestionResponse> result = service.replaceQuestions(QUIZ_ID, new UpdateQuizQuestionsRequest(List.of(
				new QuizQuestionItemRequest(SECOND_QUESTION_ID, 2, new BigDecimal("1.50")),
				new QuizQuestionItemRequest(FIRST_QUESTION_ID, 1, new BigDecimal("2.00")))), CURRENT_USER_ID);

		InOrder order = inOrder(quizQuestionRepository);
		order.verify(quizQuestionRepository).deleteByIdQuizId(QUIZ_ID);
		List<QuizQuestion> saved = captureSavedLinks(order);
		assertThat(saved).extracting(QuizQuestion::getId).containsExactly(
				new QuizQuestionId(QUIZ_ID, SECOND_QUESTION_ID),
				new QuizQuestionId(QUIZ_ID, FIRST_QUESTION_ID));
		assertThat(saved).extracting(QuizQuestion::getQuiz).containsOnly(quiz);
		assertThat(saved).extracting(QuizQuestion::getQuestion).containsExactly(second, first);
		assertThat(saved).extracting(QuizQuestion::getSortOrder).containsExactly(2, 1);
		assertThat(saved).extracting(QuizQuestion::getPoints)
				.containsExactly(new BigDecimal("1.50"), new BigDecimal("2.00"));
		assertThat(result).extracting(QuizQuestionResponse::questionId)
				.containsExactly(FIRST_QUESTION_ID, SECOND_QUESTION_ID);
		assertThat(result).extracting(QuizQuestionResponse::sortOrder).containsExactly(1, 2);
		assertThat(quiz.getUpdatedBy()).isEqualTo(CURRENT_USER_ID);
		assertThat(quiz.getUpdatedAt()).isAfter(CREATED_AT);
		verify(auditLogService).record("UPDATE_QUIZ_QUESTIONS", "quiz", QUIZ_ID);
	}

	@Test
	void replaceQuestionsRejectsTheSameQuestionTwice() {
		givenQuiz(QuizStatus.Draft);
		UpdateQuizQuestionsRequest request = new UpdateQuizQuestionsRequest(List.of(
				new QuizQuestionItemRequest(FIRST_QUESTION_ID, 1, BigDecimal.ONE),
				new QuizQuestionItemRequest(FIRST_QUESTION_ID, 2, BigDecimal.ONE)));

		assertThatThrownBy(() -> service.replaceQuestions(QUIZ_ID, request, CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Duplicate question in quiz")
				.extracting("code")
				.isEqualTo("DUPLICATE_QUIZ_QUESTION");

		assertQuestionSetUntouched();
		verify(questionRepository, never()).findAllById(anyIterable());
	}

	@Test
	void replaceQuestionsRejectsTheSameSortOrderTwice() {
		givenQuiz(QuizStatus.Draft);
		UpdateQuizQuestionsRequest request = new UpdateQuizQuestionsRequest(List.of(
				new QuizQuestionItemRequest(FIRST_QUESTION_ID, 1, BigDecimal.ONE),
				new QuizQuestionItemRequest(SECOND_QUESTION_ID, 1, BigDecimal.ONE)));

		assertThatThrownBy(() -> service.replaceQuestions(QUIZ_ID, request, CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Duplicate question sort order in quiz")
				.extracting("code")
				.isEqualTo("DUPLICATE_QUIZ_QUESTION_SORT_ORDER");

		assertQuestionSetUntouched();
	}

	@Test
	void replaceQuestionsRejectsUnknownQuestionBeforeDeletingTheOldSet() {
		givenQuiz(QuizStatus.Draft);
		when(questionRepository.findAllById(anyIterable())).thenReturn(List.of(question(FIRST_QUESTION_ID)));
		UpdateQuizQuestionsRequest request = new UpdateQuizQuestionsRequest(List.of(
				new QuizQuestionItemRequest(FIRST_QUESTION_ID, 1, BigDecimal.ONE),
				new QuizQuestionItemRequest(SECOND_QUESTION_ID, 2, BigDecimal.ONE)));

		assertThatThrownBy(() -> service.replaceQuestions(QUIZ_ID, request, CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("One or more questions were not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		assertQuestionSetUntouched();
	}

	@ParameterizedTest(name = "the questions of a {0} quiz cannot be replaced")
	@EnumSource(value = QuizStatus.class, names = {"Published", "Closed"})
	void replaceQuestionsRejectsNonDraftQuiz(QuizStatus status) {
		givenQuiz(status);
		UpdateQuizQuestionsRequest request = new UpdateQuizQuestionsRequest(List.of(
				new QuizQuestionItemRequest(FIRST_QUESTION_ID, 1, BigDecimal.ONE)));

		assertThatThrownBy(() -> service.replaceQuestions(QUIZ_ID, request, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("QUIZ_NOT_EDITABLE");

		assertQuestionSetUntouched();
		verify(questionRepository, never()).findAllById(anyIterable());
	}

	@Test
	void replaceQuestionsRejectsUnknownQuiz() {
		when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.empty());
		UpdateQuizQuestionsRequest request = new UpdateQuizQuestionsRequest(List.of(
				new QuizQuestionItemRequest(FIRST_QUESTION_ID, 1, BigDecimal.ONE)));

		assertThatThrownBy(() -> service.replaceQuestions(QUIZ_ID, request, CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz not found");

		assertQuestionSetUntouched();
	}

	private void assertQuestionSetUntouched() {
		verify(quizQuestionRepository, never()).deleteByIdQuizId(anyLong());
		verify(quizQuestionRepository, never()).saveAll(anyIterable());
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	private CreateQuizRequest createRequest(String description, LocalDate openDate, LocalDate closeDate) {
		return new CreateQuizRequest("Java basics", description, 30, 70, 2, false, "java", openDate, closeDate);
	}

	private UpdateQuizRequest updateRequest(LocalDate openDate, LocalDate closeDate) {
		return new UpdateQuizRequest("Changed title", null, 30, 70, 2, false, "java", openDate, closeDate);
	}

	private Quiz givenQuiz(QuizStatus status) {
		Quiz quiz = new Quiz();
		quiz.setId(QUIZ_ID);
		quiz.setTitle("Original title");
		quiz.setDurationMinutes(20);
		quiz.setPassingScore(50);
		quiz.setMaxAttempts(1);
		quiz.setCategory("java");
		quiz.setStatus(status);
		quiz.setCreatedAt(CREATED_AT);
		quiz.setUpdatedAt(CREATED_AT);
		quiz.setCreatedBy(AUTHOR_ID);
		quiz.setUpdatedBy(AUTHOR_ID);
		when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
		return quiz;
	}

	private Question question(long id) {
		Question question = new Question();
		question.setId(id);
		question.setContent("Question " + id);
		question.setQuestionType(QuestionType.single);
		question.setOptionsJson("[\"A\",\"B\"]");
		question.setCorrectAnswersJson("[\"A\"]");
		return question;
	}

	private QuizQuestion quizQuestion(Quiz quiz, Question question, int sortOrder, BigDecimal points) {
		QuizQuestion quizQuestion = new QuizQuestion();
		quizQuestion.setId(new QuizQuestionId(quiz.getId(), question.getId()));
		quizQuestion.setQuiz(quiz);
		quizQuestion.setQuestion(question);
		quizQuestion.setSortOrder(sortOrder);
		quizQuestion.setPoints(points);
		return quizQuestion;
	}

	private Quiz captureSaved() {
		ArgumentCaptor<Quiz> saved = ArgumentCaptor.forClass(Quiz.class);
		verify(quizRepository).save(saved.capture());
		return saved.getValue();
	}

	@SuppressWarnings("unchecked")
	private List<QuizQuestion> captureSavedLinks(InOrder order) {
		ArgumentCaptor<List<QuizQuestion>> saved = ArgumentCaptor.forClass(List.class);
		order.verify(quizQuestionRepository).saveAll(saved.capture());
		return saved.getValue();
	}
}
