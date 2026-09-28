package com.fap.training.service;

import com.fap.clazz.dto.ClassResponse;
import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassEnrollmentStatus;
import com.fap.clazz.mapper.ClassMapper;
import com.fap.clazz.repository.ClassRepository;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.NotFoundException;
import com.fap.program.entity.TrainingProgram;
import com.fap.program.entity.TrainingProgramSyllabus;
import com.fap.program.repository.TrainingProgramSyllabusRepository;
import com.fap.quiz.dto.AssignedQuizResponse;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.entity.QuizAttempt;
import com.fap.quiz.enums.QuizStatus;
import com.fap.quiz.mapper.QuizAttemptMapper;
import com.fap.quiz.repository.QuizAttemptRepository;
import com.fap.quiz.repository.QuizQuestionRepository;
import com.fap.quiz.repository.QuizRepository;
import com.fap.syllabus.dto.AssignedMaterialFileResponse;
import com.fap.syllabus.entity.MaterialFile;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.mapper.MaterialFileMapper;
import com.fap.syllabus.repository.MaterialFileRepository;
import com.fap.training.dto.MyClassDetailResponse;
import com.fap.training.dto.MyClassLearningContentResponse;
import com.fap.training.dto.MyClassProgressResponse;
import com.fap.training.dto.MyClassSyllabusResponse;
import com.fap.training.dto.MyTrainingSessionResponse;
import com.fap.training.entity.TrainingRegistration;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.AttendanceStatus;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.mapper.MyTrainingMapper;
import com.fap.training.repository.AttendanceRecordRepository;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The trainee's own view of a class. Every class-scoped read goes through the enrollment lookup
 * first, so a trainee who is not (or no longer) enrolled gets a 404 instead of another class's
 * sessions, materials and quizzes. The quiz statistics are batched into a fixed number of queries
 * for the whole class (the SQL budget in {@code QueryBaselineIT} depends on it), so the tests also
 * pin which batch queries run and when they are skipped.
 */
class MyLearningServiceTest {

	private static final long USER_ID = 7L;
	private static final long CLASS_ID = 31L;
	private static final long PROGRAM_ID = 12L;
	private static final List<ClassEnrollmentStatus> ELIGIBLE_ENROLLMENTS =
			List.of(ClassEnrollmentStatus.Enrolled, ClassEnrollmentStatus.Completed);
	private static final List<TrainingRegistrationStatus> ELIGIBLE_REGISTRATIONS =
			List.of(TrainingRegistrationStatus.Registered, TrainingRegistrationStatus.Completed);
	private static final LocalDateTime EARLIER = LocalDateTime.of(2026, 3, 2, 9, 0);
	private static final LocalDateTime LATER = LocalDateTime.of(2026, 3, 9, 9, 0);

	private final ClassRepository classRepository = mock(ClassRepository.class);
	private final TrainingRegistrationRepository trainingRegistrationRepository =
			mock(TrainingRegistrationRepository.class);
	private final AttendanceRecordRepository attendanceRecordRepository = mock(AttendanceRecordRepository.class);
	private final TrainingProgramSyllabusRepository trainingProgramSyllabusRepository =
			mock(TrainingProgramSyllabusRepository.class);
	private final MaterialFileRepository materialFileRepository = mock(MaterialFileRepository.class);
	private final QuizRepository quizRepository = mock(QuizRepository.class);
	private final QuizQuestionRepository quizQuestionRepository = mock(QuizQuestionRepository.class);
	private final QuizAttemptRepository quizAttemptRepository = mock(QuizAttemptRepository.class);
	private final ClassMapper classMapper = mock(ClassMapper.class);
	private final MyTrainingMapper myTrainingMapper = mock(MyTrainingMapper.class);
	private final MaterialFileMapper materialFileMapper = mock(MaterialFileMapper.class);
	// Real mapper: the per-quiz counts it receives are exactly what the batching must get right.
	private final QuizAttemptMapper quizAttemptMapper = new QuizAttemptMapper(new ObjectMapper());

	private final MyLearningService service = new MyLearningService(
			classRepository,
			trainingRegistrationRepository,
			attendanceRecordRepository,
			trainingProgramSyllabusRepository,
			materialFileRepository,
			quizRepository,
			quizQuestionRepository,
			quizAttemptRepository,
			classMapper,
			myTrainingMapper,
			materialFileMapper,
			quizAttemptMapper);

	private final ClassResponse classInfo = mock(ClassResponse.class);
	private LocalDate todayBeforeCall;

	@BeforeEach
	void mapClassesAndMaterials() {
		when(classMapper.toResponse(any())).thenReturn(classInfo);
		when(materialFileMapper.toAssignedResponse(any()))
				.thenAnswer(invocation -> assignedMaterial(invocation.getArgument(0)));
	}

	@BeforeEach
	void rememberToday() {
		todayBeforeCall = LocalDate.now();
	}

	@Test
	void classesSearchesEnrolledOrCompletedClassesNewestFirst() {
		FapClass fapClass = fapClass();
		when(classRepository.searchMine(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(fapClass)));

		Page<ClassResponse> page = service.classes(USER_ID, "   ", 0, 10);

		assertThat(page.getContent()).containsExactly(classInfo);
		verify(classRepository).searchMine(
				USER_ID,
				ELIGIBLE_ENROLLMENTS,
				null,
				PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
	}

	@Test
	void classesTrimsKeywordAndAppliesWhitelistedSort() {
		when(classRepository.searchMine(any(), any(), any(), any())).thenReturn(Page.empty());

		service.classes(USER_ID, " java ", 1, 20, "classCode", "asc");

		verify(classRepository).searchMine(
				USER_ID,
				ELIGIBLE_ENROLLMENTS,
				"java",
				PageRequest.of(1, 20, Sort.by(Sort.Direction.ASC, "classCode")));
	}

	@Test
	void classesRejectsUnknownSortField() {
		assertThatThrownBy(() -> service.classes(USER_ID, null, 0, 10, "trainingProgram", "asc"))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_SORT_FIELD");

		verifyNoInteractions(classRepository);
	}

	/**
	 * The enrollment lookup is the only ownership check on these reads: when it finds nothing, not a
	 * single class-content query may run.
	 */
	@ParameterizedTest(name = "{0} rejects a class the trainee is not enrolled in")
	@MethodSource("classScopedReads")
	void classScopedReadRejectsClassNotEnrolledIn(ClassScopedRead read) {
		when(classRepository.findMineById(CLASS_ID, USER_ID, ELIGIBLE_ENROLLMENTS)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> read.run(service))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Class not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		verify(classRepository).findMineById(CLASS_ID, USER_ID, ELIGIBLE_ENROLLMENTS);
		verifyNoInteractions(
				trainingRegistrationRepository,
				attendanceRecordRepository,
				trainingProgramSyllabusRepository,
				materialFileRepository,
				quizRepository,
				quizQuestionRepository,
				quizAttemptRepository);
	}

	@Test
	void classDetailListsProgramSyllabusesInProgramOrder() {
		givenMyClass();
		when(trainingProgramSyllabusRepository.findByIdProgramIdOrderBySortOrderAsc(PROGRAM_ID)).thenReturn(List.of(
				programSyllabus(syllabus(81L, "Java Core", "JAVA-01", "v2.0", SyllabusStatus.Active, "Beginner", "5 days"), 1),
				programSyllabus(syllabus(82L, "Spring Boot", "SPR-01", "v1.0", SyllabusStatus.Inactive, "Advanced", "3 days"), 2)));

		MyClassDetailResponse detail = service.classDetail(CLASS_ID, USER_ID);

		assertThat(detail.classInfo()).isSameAs(classInfo);
		assertThat(detail.syllabuses()).containsExactly(
				new MyClassSyllabusResponse(81L, "Java Core", "JAVA-01", "v2.0", SyllabusStatus.Active, "Beginner", "5 days", 1),
				new MyClassSyllabusResponse(82L, "Spring Boot", "SPR-01", "v1.0", SyllabusStatus.Inactive, "Advanced", "3 days", 2));
	}

	@Test
	void learningContentListsSessionsOfRegisteredOrCompletedRegistrations() {
		givenMyClass();
		TrainingRegistration registration = registration(TrainingSessionStatus.Upcoming);
		MyTrainingSessionResponse mappedSession = mock(MyTrainingSessionResponse.class);
		when(trainingRegistrationRepository.findMineByClassId(USER_ID, CLASS_ID, ELIGIBLE_REGISTRATIONS))
				.thenReturn(List.of(registration));
		when(myTrainingMapper.toSessionResponse(registration)).thenReturn(mappedSession);

		MyClassLearningContentResponse content = service.learningContent(CLASS_ID, USER_ID, null);

		assertThat(content.classInfo()).isSameAs(classInfo);
		assertThat(content.sessions()).containsExactly(mappedSession);
	}

	@Test
	void learningContentOrdersMaterialsNewestFirstWithUndatedLast() {
		givenMyClass();
		when(materialFileRepository.findAssignedToUserByClass(USER_ID, CLASS_ID, ELIGIBLE_ENROLLMENTS, null))
				.thenReturn(List.of(
						material(3L, null),
						material(1L, EARLIER),
						material(2L, LATER),
						material(5L, null),
						material(4L, LATER)));

		MyClassLearningContentResponse content = service.learningContent(CLASS_ID, USER_ID, null);

		// Same upload time or no upload time: the higher (newer) id comes first.
		assertThat(content.materials())
				.extracting(AssignedMaterialFileResponse::id)
				.containsExactly(4L, 2L, 1L, 5L, 3L);
	}

	/** Only the material list is searchable; sessions and quizzes stay complete whatever the keyword. */
	@ParameterizedTest(name = "keyword [{0}] reaches the materials query as [{1}]")
	@CsvSource({
			"' sql ', sql",
			"'   ',",
			","
	})
	void learningContentFiltersOnlyMaterialsByNormalizedKeyword(String keyword, String expectedKeyword) {
		givenMyClass();
		when(trainingRegistrationRepository.findMineByClassId(USER_ID, CLASS_ID, ELIGIBLE_REGISTRATIONS))
				.thenReturn(List.of(registration(TrainingSessionStatus.Upcoming)));
		givenAssignedQuizzes(quiz(10L, null));

		MyClassLearningContentResponse content = service.learningContent(CLASS_ID, USER_ID, keyword);

		verify(materialFileRepository).findAssignedToUserByClass(USER_ID, CLASS_ID, ELIGIBLE_ENROLLMENTS, expectedKeyword);
		assertThat(content.sessions()).hasSize(1);
		assertThat(content.quizzes()).extracting(AssignedQuizResponse::id).containsExactly(10L);
	}

	@Test
	void learningContentOrdersQuizzesByCloseDateWithOpenEndedLast() {
		givenMyClass();
		givenAssignedQuizzes(
				quiz(1L, null),
				quiz(2L, LocalDate.of(2026, 5, 1)),
				quiz(5L, LocalDate.of(2026, 4, 1)),
				quiz(9L, LocalDate.of(2026, 4, 1)));

		MyClassLearningContentResponse content = service.learningContent(CLASS_ID, USER_ID, null);

		// Same close date: the higher id comes first.
		assertThat(content.quizzes())
				.extracting(AssignedQuizResponse::id)
				.containsExactly(9L, 5L, 2L, 1L);
	}

	@Test
	void learningContentAsksForPublishedQuizzesOpenToday() {
		givenMyClass();

		service.learningContent(CLASS_ID, USER_ID, null);

		ArgumentCaptor<LocalDate> today = ArgumentCaptor.forClass(LocalDate.class);
		verify(quizRepository).findAssignedToUserByClass(
				eq(USER_ID),
				eq(CLASS_ID),
				eq(QuizStatus.Published),
				eq(ELIGIBLE_REGISTRATIONS),
				today.capture());
		assertIsToday(today.getValue());
	}

	@Test
	void learningContentCombinesBatchedQuizStatistics() {
		givenMyClass();
		Quiz attempted = quiz(10L, LocalDate.of(2026, 4, 1));
		attempted.setMaxAttempts(3);
		Quiz untouched = quiz(20L, null);
		givenAssignedQuizzes(attempted, untouched);
		QuizAttempt latest = attempt(99L, LATER, 60, false);
		givenAttemptStats(stats(10L, 2, 0, 99L));
		when(quizAttemptRepository.findAllById(List.of(99L))).thenReturn(List.of(latest));
		givenQuestionCounts(questionCount(10L, 5));

		List<AssignedQuizResponse> quizzes = service.learningContent(CLASS_ID, USER_ID, null).quizzes();

		assertThat(quizzes).hasSize(2);
		AssignedQuizResponse first = quizzes.get(0);
		assertThat(first.id()).isEqualTo(10L);
		assertThat(first.questionCount()).isEqualTo(5);
		assertThat(first.attemptCount()).isEqualTo(2);
		assertThat(first.remainingAttempts()).isEqualTo(1);
		assertThat(first.latestAttemptId()).isEqualTo(99L);
		assertThat(first.latestScore()).isEqualTo(60);
		assertThat(first.latestPassed()).isFalse();
		AssignedQuizResponse second = quizzes.get(1);
		assertThat(second.id()).isEqualTo(20L);
		assertThat(second.questionCount()).isZero();
		assertThat(second.attemptCount()).isZero();
		assertThat(second.latestAttemptId()).isNull();
		assertThat(second.latestScore()).isNull();
		// One grouped query each for the whole class, never one per quiz.
		verify(quizAttemptRepository).summarizeForUserByQuiz(USER_ID, List.of(10L, 20L));
		verify(quizAttemptRepository).findAllById(List.of(99L));
		verify(quizQuestionRepository).countGroupedByQuizId(List.of(10L, 20L));
	}

	@Test
	void learningContentSkipsQuizStatisticsWhenNoQuizIsAssigned() {
		givenMyClass();

		MyClassLearningContentResponse content = service.learningContent(CLASS_ID, USER_ID, null);

		assertThat(content.quizzes()).isEmpty();
		verifyNoInteractions(quizAttemptRepository, quizQuestionRepository);
	}

	@Test
	void learningContentSkipsLatestAttemptLookupWhenNothingWasAttempted() {
		givenMyClass();
		givenAssignedQuizzes(quiz(10L, null));

		List<AssignedQuizResponse> quizzes = service.learningContent(CLASS_ID, USER_ID, null).quizzes();

		assertThat(quizzes).singleElement().satisfies(quiz -> {
			assertThat(quiz.attemptCount()).isZero();
			assertThat(quiz.latestAttemptId()).isNull();
		});
		verify(quizAttemptRepository).summarizeForUserByQuiz(USER_ID, List.of(10L));
		verify(quizAttemptRepository, never()).findAllById(any());
	}

	@Test
	void progressCountsSessionsByStatus() {
		givenMyClass();
		// Each status gets a different count so a session counted in the wrong bucket changes the result.
		when(trainingRegistrationRepository.findMineByClassId(USER_ID, CLASS_ID, ELIGIBLE_REGISTRATIONS))
				.thenReturn(List.of(
						registration(TrainingSessionStatus.Completed),
						registration(TrainingSessionStatus.Upcoming),
						registration(TrainingSessionStatus.Completed),
						registration(TrainingSessionStatus.Canceled),
						registration(TrainingSessionStatus.Upcoming),
						registration(TrainingSessionStatus.Completed)));

		MyClassProgressResponse progress = service.progress(CLASS_ID, USER_ID);

		assertThat(progress.classInfo()).isSameAs(classInfo);
		assertThat(progress.sessions()).isEqualTo(new MyClassProgressResponse.SessionProgress(6, 3, 2, 1));
	}

	@Test
	void progressDefaultsMissingAttendanceStatusesToZero() {
		givenMyClass();
		AttendanceRecordRepository.StatusCount present = mock(AttendanceRecordRepository.StatusCount.class);
		when(present.getStatus()).thenReturn(AttendanceStatus.Present);
		when(present.getTotal()).thenReturn(3L);
		when(attendanceRecordRepository.countMineByClassIdGroupedByStatus(USER_ID, CLASS_ID)).thenReturn(List.of(present));

		MyClassProgressResponse progress = service.progress(CLASS_ID, USER_ID);

		assertThat(progress.attendance()).isEqualTo(new MyClassProgressResponse.AttendanceProgress(3, 0, 0));
	}

	@Test
	void progressCountsEveryAssignedMaterialWithoutKeyword() {
		givenMyClass();
		when(materialFileRepository.findAssignedToUserByClass(USER_ID, CLASS_ID, ELIGIBLE_ENROLLMENTS, null))
				.thenReturn(List.of(material(1L, EARLIER), material(2L, LATER), material(3L, null)));

		MyClassProgressResponse progress = service.progress(CLASS_ID, USER_ID);

		assertThat(progress.materials().total()).isEqualTo(3);
		verifyNoInteractions(materialFileMapper);
	}

	@Test
	void progressSummarizesAttemptedPassedAndRemainingQuizzes() {
		givenMyClass();
		givenAssignedQuizzes(quiz(10L, null), quiz(20L, null), quiz(30L, null));
		givenAttemptStats(stats(10L, 1, 1, 101L), stats(20L, 2, 0, 102L));
		when(quizAttemptRepository.findAllById(any())).thenReturn(List.of(
				attempt(101L, EARLIER, 90, true),
				attempt(102L, LATER, 40, false)));

		MyClassProgressResponse.QuizProgress quizzes = service.progress(CLASS_ID, USER_ID).quizzes();

		assertThat(quizzes.assigned()).isEqualTo(3);
		assertThat(quizzes.attempted()).isEqualTo(2);
		assertThat(quizzes.passed()).isEqualTo(1);
		assertThat(quizzes.remaining()).isEqualTo(1);
		verifyNoInteractions(quizQuestionRepository);
	}

	/** The newest attempt is chosen by start time, not by id: ids come from a pooled sequence. */
	@Test
	void progressReportsMostRecentlyStartedAttemptAcrossQuizzes() {
		givenMyClass();
		givenAssignedQuizzes(quiz(10L, null), quiz(20L, null));
		givenAttemptStats(stats(10L, 1, 1, 101L), stats(20L, 1, 0, 102L));
		when(quizAttemptRepository.findAllById(any())).thenReturn(List.of(
				attempt(101L, LATER, 85, true),
				attempt(102L, EARLIER, 40, false)));

		MyClassProgressResponse.QuizProgress quizzes = service.progress(CLASS_ID, USER_ID).quizzes();

		assertThat(quizzes.latestAttemptId()).isEqualTo(101L);
		assertThat(quizzes.latestScore()).isEqualTo(85);
		assertThat(quizzes.latestPassed()).isTrue();
	}

	@Test
	void progressBreaksLatestAttemptStartTimeTieByHigherId() {
		givenMyClass();
		givenAssignedQuizzes(quiz(10L, null), quiz(20L, null));
		givenAttemptStats(stats(10L, 1, 1, 101L), stats(20L, 1, 0, 102L));
		when(quizAttemptRepository.findAllById(any())).thenReturn(List.of(
				attempt(101L, LATER, 85, true),
				attempt(102L, LATER, 40, false)));

		MyClassProgressResponse.QuizProgress quizzes = service.progress(CLASS_ID, USER_ID).quizzes();

		assertThat(quizzes.latestAttemptId()).isEqualTo(102L);
		assertThat(quizzes.latestScore()).isEqualTo(40);
		assertThat(quizzes.latestPassed()).isFalse();
	}

	@Test
	void progressReportsNoLatestAttemptWhenNothingWasAttempted() {
		givenMyClass();
		givenAssignedQuizzes(quiz(10L, null));

		MyClassProgressResponse.QuizProgress quizzes = service.progress(CLASS_ID, USER_ID).quizzes();

		assertThat(quizzes).isEqualTo(new MyClassProgressResponse.QuizProgress(1, 0, 0, 1, null, null, null));
		verify(quizAttemptRepository, never()).findAllById(any());
	}

	@FunctionalInterface
	interface ClassScopedRead {
		void run(MyLearningService service);
	}

	static Stream<Arguments> classScopedReads() {
		return Stream.of(
				Arguments.of(Named.of("classDetail",
						(ClassScopedRead) service -> service.classDetail(CLASS_ID, USER_ID))),
				Arguments.of(Named.of("learningContent",
						(ClassScopedRead) service -> service.learningContent(CLASS_ID, USER_ID, "sql"))),
				Arguments.of(Named.of("progress",
						(ClassScopedRead) service -> service.progress(CLASS_ID, USER_ID))));
	}

	private void assertIsToday(LocalDate date) {
		// Bracketing the call keeps the check stable when the test runs across midnight.
		assertThat(date).isBetween(todayBeforeCall, LocalDate.now());
	}

	private FapClass givenMyClass() {
		FapClass fapClass = fapClass();
		when(classRepository.findMineById(CLASS_ID, USER_ID, ELIGIBLE_ENROLLMENTS)).thenReturn(Optional.of(fapClass));
		return fapClass;
	}

	private void givenAssignedQuizzes(Quiz... quizzes) {
		when(quizRepository.findAssignedToUserByClass(
				eq(USER_ID), eq(CLASS_ID), eq(QuizStatus.Published), eq(ELIGIBLE_REGISTRATIONS), any(LocalDate.class)))
				.thenReturn(Arrays.asList(quizzes));
	}

	private void givenAttemptStats(QuizAttemptRepository.UserQuizAttemptStats... stats) {
		when(quizAttemptRepository.summarizeForUserByQuiz(eq(USER_ID), any())).thenReturn(Arrays.asList(stats));
	}

	private void givenQuestionCounts(QuizQuestionRepository.QuizQuestionCount... counts) {
		when(quizQuestionRepository.countGroupedByQuizId(any())).thenReturn(Arrays.asList(counts));
	}

	private static FapClass fapClass() {
		TrainingProgram program = new TrainingProgram();
		program.setId(PROGRAM_ID);
		FapClass fapClass = new FapClass();
		fapClass.setId(CLASS_ID);
		fapClass.setTrainingProgram(program);
		return fapClass;
	}

	private static Syllabus syllabus(
			Long id,
			String name,
			String code,
			String version,
			SyllabusStatus status,
			String levelName,
			String duration) {
		Syllabus syllabus = new Syllabus();
		syllabus.setId(id);
		syllabus.setName(name);
		syllabus.setCode(code);
		syllabus.setVersion(version);
		syllabus.setStatus(status);
		syllabus.setLevelName(levelName);
		syllabus.setDuration(duration);
		return syllabus;
	}

	private static TrainingProgramSyllabus programSyllabus(Syllabus syllabus, int sortOrder) {
		TrainingProgramSyllabus programSyllabus = new TrainingProgramSyllabus();
		programSyllabus.setSyllabus(syllabus);
		programSyllabus.setSortOrder(sortOrder);
		return programSyllabus;
	}

	private static TrainingRegistration registration(TrainingSessionStatus sessionStatus) {
		TrainingSession session = new TrainingSession();
		session.setStatus(sessionStatus);
		TrainingRegistration registration = new TrainingRegistration();
		registration.setTrainingSession(session);
		return registration;
	}

	private static MaterialFile material(Long id, LocalDateTime uploadedAt) {
		MaterialFile material = new MaterialFile();
		material.setId(id);
		material.setUploadedAt(uploadedAt);
		return material;
	}

	private static AssignedMaterialFileResponse assignedMaterial(MaterialFile material) {
		return new AssignedMaterialFileResponse(
				material.getId(), null, null, null, null, null, null, null, null, null, null,
				material.getUploadedAt(), false);
	}

	private static Quiz quiz(Long id, LocalDate closeDate) {
		Quiz quiz = new Quiz();
		quiz.setId(id);
		quiz.setTitle("Quiz " + id);
		quiz.setStatus(QuizStatus.Published);
		quiz.setCloseDate(closeDate);
		return quiz;
	}

	private static QuizAttempt attempt(Long id, LocalDateTime startedAt, int score, boolean passed) {
		QuizAttempt attempt = new QuizAttempt();
		attempt.setId(id);
		attempt.setStartedAt(startedAt);
		attempt.setScore(score);
		attempt.setPassed(passed);
		return attempt;
	}

	private static QuizAttemptRepository.UserQuizAttemptStats stats(
			Long quizId,
			long attempts,
			long passedAttempts,
			Long latestAttemptId) {
		// Every getter is stubbed: the service collects these into maps, which reject null values.
		QuizAttemptRepository.UserQuizAttemptStats stats = mock(QuizAttemptRepository.UserQuizAttemptStats.class);
		when(stats.getQuizId()).thenReturn(quizId);
		when(stats.getAttempts()).thenReturn(attempts);
		when(stats.getPassedAttempts()).thenReturn(passedAttempts);
		when(stats.getLatestAttemptId()).thenReturn(latestAttemptId);
		return stats;
	}

	private static QuizQuestionRepository.QuizQuestionCount questionCount(Long quizId, long total) {
		QuizQuestionRepository.QuizQuestionCount count = mock(QuizQuestionRepository.QuizQuestionCount.class);
		when(count.getQuizId()).thenReturn(quizId);
		when(count.getTotal()).thenReturn(total);
		return count;
	}
}
