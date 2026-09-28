package com.fap.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.NotFoundException;
import com.fap.quiz.dto.CreateQuestionRequest;
import com.fap.quiz.dto.QuestionResponse;
import com.fap.quiz.dto.UpdateQuestionRequest;
import com.fap.quiz.entity.Question;
import com.fap.quiz.enums.QuestionDifficulty;
import com.fap.quiz.enums.QuestionType;
import com.fap.quiz.mapper.QuestionMapper;
import com.fap.quiz.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The question bank feeds grading: a question whose options or answer key is not a usable JSON
 * array, or a single-choice question with several keys, can never be answered correctly. These
 * tests pin the validation, the field normalization and the soft delete that keeps past attempts
 * pointing at a real row.
 */
class QuestionServiceTest {

	private static final long QUESTION_ID = 501L;
	private static final long AUTHOR_ID = 1L;
	private static final long CURRENT_USER_ID = 7L;
	private static final LocalDateTime CREATED_AT = LocalDateTime.of(2020, 1, 5, 8, 0);
	private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

	private final QuestionRepository questionRepository = mock(QuestionRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final ObjectMapper objectMapper = new ObjectMapper();

	private final QuestionService service = new QuestionService(
			questionRepository,
			new QuestionMapper(objectMapper),
			objectMapper,
			auditLogService);

	@BeforeEach
	void setUp() {
		lenient().doCallRealMethod().when(questionRepository).getQuestionOrThrow(any());
		when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
			Question saved = invocation.getArgument(0);
			saved.setId(QUESTION_ID);
			return saved;
		});
	}

	@Test
	void createTrimsFieldsStoresJsonAndAudits() {
		CreateQuestionRequest request = new CreateQuestionRequest(
				"  What is the JVM?  ",
				QuestionType.single,
				"  java  ",
				QuestionDifficulty.Easy,
				json("[\"A\",\"B\"]"),
				json("[\"A\"]"),
				"   ");

		QuestionResponse response = service.create(request, CURRENT_USER_ID);

		Question saved = captureSaved();
		assertThat(saved.getContent()).isEqualTo("What is the JVM?");
		assertThat(saved.getCategory()).isEqualTo("java");
		assertThat(saved.getQuestionType()).isEqualTo(QuestionType.single);
		assertThat(saved.getDifficulty()).isEqualTo(QuestionDifficulty.Easy);
		assertThat(saved.getOptionsJson()).isEqualTo("[\"A\",\"B\"]");
		assertThat(saved.getCorrectAnswersJson()).isEqualTo("[\"A\"]");
		assertThat(saved.getExplanation()).isNull();
		assertThat(saved.isDeleted()).isFalse();
		assertThat(saved.getCreatedBy()).isEqualTo(CURRENT_USER_ID);
		assertThat(saved.getUpdatedBy()).isEqualTo(CURRENT_USER_ID);
		assertThat(saved.getCreatedAt()).isNotNull().isEqualTo(saved.getUpdatedAt());
		verify(auditLogService).record("CREATE_QUESTION", "question", QUESTION_ID);
		assertThat(response.id()).isEqualTo(QUESTION_ID);
		assertThat(response.optionsJson()).isEqualTo(json("[\"A\",\"B\"]"));
		assertThat(response.correctAnswersJson()).isEqualTo(json("[\"A\"]"));
	}

	@Test
	void createTrimsAPresentExplanation() {
		service.create(createRequest(QuestionType.single, json("[\"A\",\"B\"]"), json("[\"A\"]"), "  Because A  "),
				CURRENT_USER_ID);

		assertThat(captureSaved().getExplanation()).isEqualTo("Because A");
	}

	@ParameterizedTest(name = "options {0} are rejected")
	@MethodSource("notANonEmptyArray")
	void createRejectsOptionsThatAreNotANonEmptyArray(String description, JsonNode options) {
		CreateQuestionRequest request = createRequest(QuestionType.single, options, json("[\"A\"]"), null);

		assertThatThrownBy(() -> service.create(request, CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Question options must be a non-empty JSON array")
				.extracting("code")
				.isEqualTo("INVALID_QUESTION_OPTIONS_JSON");

		verify(questionRepository, never()).save(any());
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@ParameterizedTest(name = "correct answers {0} are rejected")
	@MethodSource("notANonEmptyArray")
	void createRejectsCorrectAnswersThatAreNotANonEmptyArray(String description, JsonNode correctAnswers) {
		CreateQuestionRequest request = createRequest(QuestionType.multiple, json("[\"A\",\"B\"]"), correctAnswers, null);

		assertThatThrownBy(() -> service.create(request, CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Question correct answers must be a non-empty JSON array")
				.extracting("code")
				.isEqualTo("INVALID_QUESTION_CORRECT_ANSWERS_JSON");

		verify(questionRepository, never()).save(any());
	}

	@Test
	void createRejectsSingleChoiceWithSeveralCorrectAnswers() {
		CreateQuestionRequest request = createRequest(
				QuestionType.single, json("[\"A\",\"B\",\"C\"]"), json("[\"A\",\"B\"]"), null);

		assertThatThrownBy(() -> service.create(request, CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Single-choice questions must have exactly one correct answer")
				.extracting("code")
				.isEqualTo("INVALID_SINGLE_QUESTION_ANSWER");

		verify(questionRepository, never()).save(any());
	}

	@ParameterizedTest(name = "multiple choice with {0} is accepted")
	@CsvSource(delimiter = '|', value = {
			"[\"A\"]",
			"[\"A\",\"C\"]"
	})
	void createAcceptsMultipleChoiceWithAnyNumberOfCorrectAnswers(String correctAnswers) {
		service.create(createRequest(QuestionType.multiple, json("[\"A\",\"B\",\"C\"]"), json(correctAnswers), null),
				CURRENT_USER_ID);

		assertThat(captureSaved().getCorrectAnswersJson()).isEqualTo(correctAnswers);
		verify(auditLogService).record("CREATE_QUESTION", "question", QUESTION_ID);
	}

	@Test
	void createChecksOptionsBeforeCorrectAnswers() {
		CreateQuestionRequest request = createRequest(QuestionType.single, NODES.arrayNode(), NODES.arrayNode(), null);

		assertThatThrownBy(() -> service.create(request, CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_QUESTION_OPTIONS_JSON");
	}

	/**
	 * Current behaviour, not a rule: the answer key is never compared with the options, so a key the
	 * trainee cannot pick from the options is stored as-is.
	 */
	@Test
	void createDoesNotCheckThatCorrectAnswersAreAmongTheOptions() {
		service.create(createRequest(QuestionType.single, json("[\"A\",\"B\"]"), json("[\"Z\"]"), null),
				CURRENT_USER_ID);

		assertThat(captureSaved().getCorrectAnswersJson()).isEqualTo("[\"Z\"]");
	}

	@Test
	void updateAppliesFieldsKeepsAuthorAndAudits() {
		Question question = givenQuestion();

		QuestionResponse response = service.update(QUESTION_ID, new UpdateQuestionRequest(
				" Which keyword declares a constant? ",
				QuestionType.multiple,
				" java-core ",
				QuestionDifficulty.Hard,
				json("[\"final\",\"static\",\"const\"]"),
				json("[\"final\",\"static\"]"),
				" final plus static "), CURRENT_USER_ID);

		assertThat(question.getContent()).isEqualTo("Which keyword declares a constant?");
		assertThat(question.getQuestionType()).isEqualTo(QuestionType.multiple);
		assertThat(question.getCategory()).isEqualTo("java-core");
		assertThat(question.getDifficulty()).isEqualTo(QuestionDifficulty.Hard);
		assertThat(question.getOptionsJson()).isEqualTo("[\"final\",\"static\",\"const\"]");
		assertThat(question.getCorrectAnswersJson()).isEqualTo("[\"final\",\"static\"]");
		assertThat(question.getExplanation()).isEqualTo("final plus static");
		assertThat(question.getUpdatedBy()).isEqualTo(CURRENT_USER_ID);
		assertThat(question.getUpdatedAt()).isAfter(CREATED_AT);
		assertThat(question.getCreatedBy()).isEqualTo(AUTHOR_ID);
		assertThat(question.getCreatedAt()).isEqualTo(CREATED_AT);
		verify(auditLogService).record("UPDATE_QUESTION", "question", QUESTION_ID);
		assertThat(response.content()).isEqualTo("Which keyword declares a constant?");
	}

	@Test
	void updateClearsABlankExplanation() {
		Question question = givenQuestion();

		service.update(QUESTION_ID, updateRequest(QuestionType.single, json("[\"A\"]"), "  "), CURRENT_USER_ID);

		assertThat(question.getExplanation()).isNull();
	}

	@Test
	void updateValidatesLikeCreateAndLeavesTheQuestionUntouched() {
		Question question = givenQuestion();

		assertThatThrownBy(() -> service.update(
				QUESTION_ID, updateRequest(QuestionType.single, json("[\"A\",\"B\"]"), null), CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_SINGLE_QUESTION_ANSWER");

		assertThat(question.getContent()).isEqualTo("Original content");
		assertThat(question.getCorrectAnswersJson()).isEqualTo("[\"A\"]");
		assertThat(question.getUpdatedBy()).isEqualTo(AUTHOR_ID);
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@Test
	void updateRejectsUnknownQuestion() {
		when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(
				QUESTION_ID, updateRequest(QuestionType.single, json("[\"A\"]"), null), CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Question not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@Test
	void deleteSoftDeletesAndAudits() {
		Question question = givenQuestion();

		service.delete(QUESTION_ID, CURRENT_USER_ID);

		assertThat(question.isDeleted()).isTrue();
		assertThat(question.getDeletedAt()).isNotNull().isEqualTo(question.getUpdatedAt());
		assertThat(question.getUpdatedBy()).isEqualTo(CURRENT_USER_ID);
		assertThat(question.getCreatedBy()).isEqualTo(AUTHOR_ID);
		verify(questionRepository, never()).delete(any());
		verify(questionRepository, never()).deleteById(any());
		verify(auditLogService).record("DELETE_QUESTION", "question", QUESTION_ID);
	}

	@Test
	void deleteRejectsUnknownQuestion() {
		when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(QUESTION_ID, CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Question not found");

		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@Test
	void getMapsStoredJsonBackToNodes() {
		givenQuestion();

		QuestionResponse response = service.get(QUESTION_ID);

		assertThat(response.id()).isEqualTo(QUESTION_ID);
		assertThat(response.optionsJson()).isEqualTo(json("[\"A\",\"B\"]"));
		assertThat(response.correctAnswersJson()).isEqualTo(json("[\"A\"]"));
	}

	@Test
	void getRejectsUnknownQuestion() {
		when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(QUESTION_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Question not found");
	}

	@Test
	void listNormalizesFiltersAndBindsEnumsByName() {
		givenSearchReturns(List.of(existingQuestion()));

		Page<QuestionResponse> page = service.list(
				QuestionType.single, QuestionDifficulty.Easy, "   ", "  jvm  ", 0, 20);

		verify(questionRepository).search(eq("single"), eq("Easy"), isNull(), eq("jvm"), any(Pageable.class));
		assertThat(page.getContent()).extracting(QuestionResponse::id).containsExactly(QUESTION_ID);
	}

	@Test
	void listWithoutFiltersPassesNullsSoTheQuerySkipsThem() {
		givenSearchReturns(List.of());

		service.list(null, null, null, null, 0, 20);

		verify(questionRepository).search(isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
	}

	@Test
	void listDefaultsToNewestFirst() {
		givenSearchReturns(List.of());

		service.list(null, null, null, null, 2, 10);

		Pageable pageable = captureSearchPageable();
		assertThat(pageable.getPageNumber()).isEqualTo(2);
		assertThat(pageable.getPageSize()).isEqualTo(10);
		assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "id"));
	}

	/** The search is a native query, so camelCase sort fields must reach it as column names. */
	@ParameterizedTest(name = "sort by {0} uses column {1}")
	@CsvSource({
			"questionType, question_type",
			"createdAt, created_at",
			"category, category",
			"difficulty, difficulty",
			"id, id"
	})
	void listMapsSortFieldsToColumns(String sortBy, String column) {
		givenSearchReturns(List.of());

		service.list(null, null, null, null, 0, 20, sortBy, "desc");

		assertThat(captureSearchPageable().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, column));
	}

	@Test
	void listRejectsUnknownSortField() {
		assertThatThrownBy(() -> service.list(null, null, null, null, 0, 20, "content", "asc"))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_SORT_FIELD");

		verify(questionRepository, never()).search(any(), any(), any(), any(), any());
	}

	static Stream<Arguments> notANonEmptyArray() {
		return Stream.of(
				Arguments.of("missing", null),
				Arguments.of("as an object", NODES.objectNode().put("A", "x")),
				Arguments.of("as text", NODES.textNode("A")),
				Arguments.of("as an empty array", NODES.arrayNode()));
	}

	private CreateQuestionRequest createRequest(
			QuestionType questionType,
			JsonNode options,
			JsonNode correctAnswers,
			String explanation) {
		return new CreateQuestionRequest(
				"Pick one", questionType, "java", QuestionDifficulty.Medium, options, correctAnswers, explanation);
	}

	private UpdateQuestionRequest updateRequest(QuestionType questionType, JsonNode correctAnswers, String explanation) {
		return new UpdateQuestionRequest(
				"Updated content",
				questionType,
				"java",
				QuestionDifficulty.Medium,
				json("[\"A\",\"B\"]"),
				correctAnswers,
				explanation);
	}

	private Question givenQuestion() {
		Question question = existingQuestion();
		when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
		return question;
	}

	private Question existingQuestion() {
		Question question = new Question();
		question.setId(QUESTION_ID);
		question.setContent("Original content");
		question.setQuestionType(QuestionType.single);
		question.setCategory("java");
		question.setDifficulty(QuestionDifficulty.Easy);
		question.setOptionsJson("[\"A\",\"B\"]");
		question.setCorrectAnswersJson("[\"A\"]");
		question.setExplanation("Original explanation");
		question.setCreatedAt(CREATED_AT);
		question.setUpdatedAt(CREATED_AT);
		question.setCreatedBy(AUTHOR_ID);
		question.setUpdatedBy(AUTHOR_ID);
		return question;
	}

	private void givenSearchReturns(List<Question> questions) {
		when(questionRepository.search(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(questions));
	}

	private Pageable captureSearchPageable() {
		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(questionRepository).search(any(), any(), any(), any(), pageable.capture());
		return pageable.getValue();
	}

	private Question captureSaved() {
		ArgumentCaptor<Question> saved = ArgumentCaptor.forClass(Question.class);
		verify(questionRepository).save(saved.capture());
		return saved.getValue();
	}

	private JsonNode json(String value) {
		try {
			return objectMapper.readTree(value);
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("Invalid test JSON: " + value, exception);
		}
	}
}
