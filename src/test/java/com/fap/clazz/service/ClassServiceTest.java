package com.fap.clazz.service;

import com.fap.clazz.dto.ClassResponse;
import com.fap.clazz.dto.CreateClassRequest;
import com.fap.clazz.dto.UpdateClassRequest;
import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.mapper.ClassMapper;
import com.fap.clazz.repository.ClassAdminRepository;
import com.fap.clazz.repository.ClassRepository;
import com.fap.clazz.repository.ClassTrainerRepository;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.security.FapUserPrincipal;
import com.fap.common.security.RoleNames;
import com.fap.program.entity.TrainingProgram;
import com.fap.program.enums.TrainingProgramStatus;
import com.fap.program.repository.TrainingProgramRepository;
import com.fap.result.service.CourseResultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Creating, editing and listing classes. A new class always starts in Planning under an active
 * training program, only a Planning class may be edited (a running class keeps its schedule and
 * capacity), and the list is scoped so that staff other than Super Admins only see the classes
 * they are assigned to. The status lifecycle itself is covered by {@link ClassStateMachineTest}.
 */
class ClassServiceTest {

	private static final long CLASS_ID = 21L;
	private static final long PROGRAM_ID = 99L;
	private static final long CURRENT_USER_ID = 7L;
	private static final LocalDate START = LocalDate.of(2026, 10, 5);
	private static final LocalDate END = LocalDate.of(2026, 12, 18);
	private static final LocalDate ENROLLMENT_START = LocalDate.of(2026, 9, 1);
	private static final LocalDate ENROLLMENT_END = LocalDate.of(2026, 10, 2);
	private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

	private final ClassRepository classRepository = mock(ClassRepository.class);
	private final ClassAdminRepository classAdminRepository = mock(ClassAdminRepository.class);
	private final ClassTrainerRepository classTrainerRepository = mock(ClassTrainerRepository.class);
	private final TrainingProgramRepository trainingProgramRepository = mock(TrainingProgramRepository.class);
	private final ClassMapper classMapper = mock(ClassMapper.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final ClassEnrollmentService classEnrollmentService = mock(ClassEnrollmentService.class);
	private final CourseResultService courseResultService = mock(CourseResultService.class);

	private final ClassService service = new ClassService(
			classRepository,
			classAdminRepository,
			classTrainerRepository,
			trainingProgramRepository,
			classMapper,
			auditLogService,
			classEnrollmentService,
			courseResultService);

	/**
	 * The finders are the single stubbed source of truth, whether the service calls them directly or
	 * through the repositories' {@code get...OrThrow} lookup defaults.
	 */
	@BeforeEach
	void lookupDefaultsDelegateToStubbedFinders() {
		lenient().doCallRealMethod().when(classRepository).getWithTrainingProgramOrThrow(any());
		lenient().doCallRealMethod().when(trainingProgramRepository).getTrainingProgramOrThrow(any());
	}

	@BeforeEach
	void saveAssignsIdAndMapperEchoesTheClass() {
		when(classRepository.save(any(FapClass.class))).thenAnswer(invocation -> {
			FapClass fapClass = invocation.getArgument(0);
			fapClass.setId(CLASS_ID);
			return fapClass;
		});
		when(classMapper.toResponse(any(FapClass.class))).thenAnswer(invocation -> responseFor(invocation.getArgument(0)));
	}

	// --- create ---

	@Test
	void createStoresPlanningClassWithNormalizedCodeAndAuditsIt() {
		TrainingProgram program = givenProgram(TrainingProgramStatus.Active);
		LocalDateTime before = LocalDateTime.now();

		ClassResponse response = service.create(
				createRequest(" jv-01 ", START, END, ENROLLMENT_START, ENROLLMENT_END),
				CURRENT_USER_ID);

		LocalDateTime after = LocalDateTime.now();
		FapClass saved = captureSaved();
		assertThat(saved.getClassCode()).isEqualTo("JV-01");
		assertThat(saved.getName()).isEqualTo("Java Backend");
		assertThat(saved.getTrainingProgram()).isSameAs(program);
		assertThat(saved.getStatus()).isEqualTo(ClassStatus.Planning);
		assertThat(saved.getLocation()).isEqualTo("Ha Noi");
		assertThat(saved.getLocationDetail()).isEqualTo("Room 301");
		assertThat(saved.getFsu()).isEqualTo("FHN");
		assertThat(saved.getClassTime()).isEqualTo("Morning");
		assertThat(saved.getStartDate()).isEqualTo(START);
		assertThat(saved.getEndDate()).isEqualTo(END);
		assertThat(saved.getDuration()).isEqualTo("11 weeks");
		assertThat(saved.getCapacity()).isEqualTo(25);
		assertThat(saved.isSelfEnrollmentEnabled()).isTrue();
		assertThat(saved.getEnrollmentStartDate()).isEqualTo(ENROLLMENT_START);
		assertThat(saved.getEnrollmentEndDate()).isEqualTo(ENROLLMENT_END);
		assertThat(saved.getCreatedAt()).isBetween(before, after).isEqualTo(saved.getUpdatedAt());
		assertThat(saved.getCreatedBy()).isEqualTo(CURRENT_USER_ID);
		assertThat(saved.getUpdatedBy()).isEqualTo(CURRENT_USER_ID);
		verify(auditLogService).record("CREATE_CLASS", "class", CLASS_ID);
		assertThat(response.id()).isEqualTo(CLASS_ID);
		assertThat(response.classCode()).isEqualTo("JV-01");
	}

	@Test
	void createRejectsExistingClassCode() {
		givenProgram(TrainingProgramStatus.Active);
		when(classRepository.existsByClassCodeIgnoreCase("JV-01")).thenReturn(true);

		assertThatThrownBy(() -> service.create(createRequest("JV-01", START, END, null, null), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_CODE_EXISTS");

		verify(classRepository, never()).save(any());
		verifyNoInteractions(auditLogService);
	}

	/**
	 * Pins a known gap: uniqueness is checked on the code exactly as sent, while the stored code is
	 * trimmed and upper-cased. A padded copy of an existing code passes this check; in production the
	 * insert then fails on the {@code uk_classes_code} constraint and surfaces as a 500 instead of
	 * {@code CLASS_CODE_EXISTS}.
	 */
	@Test
	void createChecksCodeUniquenessBeforeTrimmingSoAPaddedDuplicateIsNotCaught() {
		givenProgram(TrainingProgramStatus.Active);
		when(classRepository.existsByClassCodeIgnoreCase("JV-01")).thenReturn(true);

		service.create(createRequest(" JV-01 ", START, END, null, null), CURRENT_USER_ID);

		verify(classRepository).existsByClassCodeIgnoreCase(" JV-01 ");
		assertThat(captureSaved().getClassCode()).isEqualTo("JV-01");
	}

	@Test
	void createRejectsStartDateAfterEndDate() {
		givenProgram(TrainingProgramStatus.Active);

		assertThatThrownBy(() -> service.create(
				createRequest("JV-01", END, START, null, null), CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_CLASS_DATE_RANGE");

		verify(classRepository, never()).save(any());
	}

	@Test
	void createRejectsEnrollmentStartAfterEnrollmentEnd() {
		givenProgram(TrainingProgramStatus.Active);

		assertThatThrownBy(() -> service.create(
				createRequest("JV-01", START, END, ENROLLMENT_END, ENROLLMENT_START), CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_CLASS_ENROLLMENT_DATE_RANGE");

		verify(classRepository, never()).save(any());
	}

	/** Both ranges are inclusive: a one-day class and a one-day enrollment window are valid. */
	@Test
	void createAcceptsSameDayStartAndEnd() {
		givenProgram(TrainingProgramStatus.Active);

		service.create(createRequest("JV-01", START, START, ENROLLMENT_START, ENROLLMENT_START), CURRENT_USER_ID);

		FapClass saved = captureSaved();
		assertThat(saved.getEndDate()).isEqualTo(START);
		assertThat(saved.getEnrollmentEndDate()).isEqualTo(ENROLLMENT_START);
	}

	/** A Planning class may still have an open schedule; the dates only become mandatory at activation. */
	@ParameterizedTest(name = "start={0}, end={1}")
	@CsvSource({
			"2026-12-18,",
			",2026-10-05",
			","
	})
	void createAcceptsOpenEndedDates(LocalDate start, LocalDate end) {
		givenProgram(TrainingProgramStatus.Active);

		service.create(createRequest("JV-01", start, end, end, start), CURRENT_USER_ID);

		FapClass saved = captureSaved();
		assertThat(saved.getStartDate()).isEqualTo(start);
		assertThat(saved.getEndDate()).isEqualTo(end);
	}

	@Test
	void createRejectsUnknownTrainingProgram() {
		when(trainingProgramRepository.findById(PROGRAM_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(createRequest("JV-01", START, END, null, null), CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Training program not found")
				.extracting("code")
				.isEqualTo(NotFoundException.CODE);

		verify(classRepository, never()).save(any());
	}

	@ParameterizedTest(name = "{0} program cannot host a new class")
	@EnumSource(value = TrainingProgramStatus.class, names = "Active", mode = EnumSource.Mode.EXCLUDE)
	void createRejectsTrainingProgramThatIsNotActive(TrainingProgramStatus programStatus) {
		givenProgram(programStatus);

		assertThatThrownBy(() -> service.create(createRequest("JV-01", START, END, null, null), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_TRAINING_PROGRAM_NOT_ACTIVE");

		verify(classRepository, never()).save(any());
		verifyNoInteractions(auditLogService);
	}

	// --- update ---

	@Test
	void updateAppliesRequestToPlanningClassAndAuditsIt() {
		FapClass fapClass = givenClass(ClassStatus.Planning);
		LocalDateTime before = LocalDateTime.now();

		ClassResponse response = service.update(CLASS_ID, new UpdateClassRequest(
				"Java Backend 2",
				"Da Nang",
				"Room 12",
				"FDN",
				"Evening",
				START,
				END,
				"11 weeks",
				40,
				false,
				ENROLLMENT_START,
				ENROLLMENT_END), CURRENT_USER_ID);

		LocalDateTime after = LocalDateTime.now();
		assertThat(fapClass.getName()).isEqualTo("Java Backend 2");
		assertThat(fapClass.getLocation()).isEqualTo("Da Nang");
		assertThat(fapClass.getLocationDetail()).isEqualTo("Room 12");
		assertThat(fapClass.getFsu()).isEqualTo("FDN");
		assertThat(fapClass.getClassTime()).isEqualTo("Evening");
		assertThat(fapClass.getStartDate()).isEqualTo(START);
		assertThat(fapClass.getEndDate()).isEqualTo(END);
		assertThat(fapClass.getDuration()).isEqualTo("11 weeks");
		assertThat(fapClass.getCapacity()).isEqualTo(40);
		assertThat(fapClass.isSelfEnrollmentEnabled()).isFalse();
		assertThat(fapClass.getEnrollmentStartDate()).isEqualTo(ENROLLMENT_START);
		assertThat(fapClass.getEnrollmentEndDate()).isEqualTo(ENROLLMENT_END);
		assertThat(fapClass.getUpdatedAt()).isBetween(before, after);
		assertThat(fapClass.getUpdatedBy()).isEqualTo(CURRENT_USER_ID);
		assertThat(fapClass.getClassCode()).isEqualTo("JV-01");
		assertThat(fapClass.getCreatedBy()).isEqualTo(1L);
		verify(classEnrollmentService).validateCapacity(CLASS_ID, 40);
		verify(auditLogService).record("UPDATE_CLASS", "class", CLASS_ID);
		assertThat(response.id()).isEqualTo(CLASS_ID);
		assertThat(response.name()).isEqualTo("Java Backend 2");
	}

	/** Name, capacity and self-enrolment are required on create, so an omitted value means "keep". */
	@Test
	void updateKeepsNameCapacityAndSelfEnrollmentWhenOmitted() {
		FapClass fapClass = givenClass(ClassStatus.Planning);

		service.update(CLASS_ID, emptyUpdateRequest(), CURRENT_USER_ID);

		assertThat(fapClass.getName()).isEqualTo("Java Backend");
		assertThat(fapClass.getCapacity()).isEqualTo(25);
		assertThat(fapClass.isSelfEnrollmentEnabled()).isTrue();
		verify(classEnrollmentService, never()).validateCapacity(anyLong(), anyInt());
		verify(auditLogService).record("UPDATE_CLASS", "class", CLASS_ID);
	}

	/**
	 * Pins current behaviour: every other field is replaced as sent, so omitting it clears it. The
	 * update is a full replace of the optional details rather than a patch.
	 */
	@Test
	void updateClearsOptionalDetailsWhenOmitted() {
		FapClass fapClass = givenClass(ClassStatus.Planning);

		service.update(CLASS_ID, emptyUpdateRequest(), CURRENT_USER_ID);

		assertThat(fapClass.getLocation()).isNull();
		assertThat(fapClass.getLocationDetail()).isNull();
		assertThat(fapClass.getFsu()).isNull();
		assertThat(fapClass.getClassTime()).isNull();
		assertThat(fapClass.getStartDate()).isNull();
		assertThat(fapClass.getEndDate()).isNull();
		assertThat(fapClass.getDuration()).isNull();
		assertThat(fapClass.getEnrollmentStartDate()).isNull();
		assertThat(fapClass.getEnrollmentEndDate()).isNull();
	}

	@Test
	void updateRejectsUnknownClass() {
		when(classRepository.findWithTrainingProgramById(CLASS_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(CLASS_ID, emptyUpdateRequest(), CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Class not found")
				.extracting("code")
				.isEqualTo(NotFoundException.CODE);

		verifyNoInteractions(classEnrollmentService, auditLogService);
	}

	@ParameterizedTest(name = "{0} class is not editable")
	@EnumSource(value = ClassStatus.class, names = "Planning", mode = EnumSource.Mode.EXCLUDE)
	void updateRejectsClassThatIsNoLongerPlanning(ClassStatus status) {
		FapClass fapClass = givenClass(status);

		assertThatThrownBy(() -> service.update(CLASS_ID, updateRequestWithCapacity(10), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_NOT_EDITABLE");

		assertThat(fapClass.getCapacity()).isEqualTo(25);
		assertThat(fapClass.getLocation()).isEqualTo("Ha Noi");
		assertThat(fapClass.getUpdatedBy()).isEqualTo(1L);
		verifyNoInteractions(classEnrollmentService, auditLogService);
	}

	/** The date checks run before the capacity check, so a bad range never reaches the enrolment count. */
	@Test
	void updateRejectsStartDateAfterEndDate() {
		FapClass fapClass = givenClass(ClassStatus.Planning);

		assertThatThrownBy(() -> service.update(CLASS_ID, new UpdateClassRequest(
				"Renamed", null, null, null, null, END, START, null, 40, null, null, null), CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_CLASS_DATE_RANGE");

		assertThat(fapClass.getName()).isEqualTo("Java Backend");
		verifyNoInteractions(classEnrollmentService, auditLogService);
	}

	@Test
	void updateRejectsEnrollmentStartAfterEnrollmentEnd() {
		FapClass fapClass = givenClass(ClassStatus.Planning);

		assertThatThrownBy(() -> service.update(CLASS_ID, new UpdateClassRequest(
				"Renamed", null, null, null, null, START, END, null, null, null, ENROLLMENT_END, ENROLLMENT_START),
				CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_CLASS_ENROLLMENT_DATE_RANGE");

		assertThat(fapClass.getName()).isEqualTo("Java Backend");
		verifyNoInteractions(auditLogService);
	}

	/** Capacity may not drop below the trainees already enrolled; the class must stay untouched. */
	@Test
	void updateRejectsCapacityBelowEnrolledCount() {
		FapClass fapClass = givenClass(ClassStatus.Planning);
		doThrow(new ConflictException(
				"CLASS_CAPACITY_BELOW_ENROLLED_COUNT",
				"Class capacity cannot be lower than the current enrolled count"))
				.when(classEnrollmentService).validateCapacity(CLASS_ID, 10);

		assertThatThrownBy(() -> service.update(CLASS_ID, updateRequestWithCapacity(10), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_CAPACITY_BELOW_ENROLLED_COUNT");

		assertThat(fapClass.getCapacity()).isEqualTo(25);
		assertThat(fapClass.getLocation()).isEqualTo("Ha Noi");
		verifyNoInteractions(auditLogService);
	}

	// --- list ---

	@Test
	void listSearchesAllClassesNewestFirstWithTrimmedKeyword() {
		FapClass first = listedClass(1L);
		FapClass second = listedClass(2L);
		when(classRepository.searchScoped(any(), any(), any(), any(), any(Pageable.class)))
				.thenAnswer(invocation -> new PageImpl<>(List.of(first, second), invocation.getArgument(4), 22));

		Page<ClassResponse> page = service.list(ClassStatus.Active, PROGRAM_ID, "  java ", 1, 20);

		verify(classRepository).searchScoped(
				isNull(),
				eq(ClassStatus.Active),
				eq(PROGRAM_ID),
				eq("java"),
				eq(PageRequest.of(1, 20, NEWEST_FIRST)));
		assertThat(page.getContent()).extracting(ClassResponse::id).containsExactly(1L, 2L);
		assertThat(page.getTotalElements()).isEqualTo(22);
	}

	@ParameterizedTest(name = "keyword \"{0}\" means no keyword filter")
	@ValueSource(strings = {"", "   "})
	void listTreatsBlankKeywordAsNoFilter(String keyword) {
		givenEmptySearch();

		service.list(null, null, keyword, 0, 10);

		verify(classRepository).searchScoped(isNull(), isNull(), isNull(), isNull(), eq(PageRequest.of(0, 10, NEWEST_FIRST)));
	}

	@Test
	void listScopedLeavesSuperAdminUnscoped() {
		givenEmptySearch();

		service.listScoped(principal(3L, RoleNames.SUPER_ADMIN, RoleNames.TRAINER), null, null, null, 0, 10);

		verify(classRepository).searchScoped(isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
	}

	/** Everyone except a Super Admin only sees the classes they are assigned to as admin or trainer. */
	@ParameterizedTest(name = "{0} is scoped to own classes")
	@ValueSource(strings = {RoleNames.CLASS_ADMIN, RoleNames.TRAINER, RoleNames.TRAINEE})
	void listScopedRestrictsOtherRolesToTheirOwnClasses(String roleName) {
		givenEmptySearch();

		service.listScoped(principal(5L, roleName), ClassStatus.Planning, PROGRAM_ID, "java", 0, 10);

		verify(classRepository).searchScoped(
				eq(5L),
				eq(ClassStatus.Planning),
				eq(PROGRAM_ID),
				eq("java"),
				eq(PageRequest.of(0, 10, NEWEST_FIRST)));
	}

	@Test
	void listScopedWithoutPrincipalIsUnscoped() {
		givenEmptySearch();

		service.listScoped(null, null, null, null, 0, 10);

		verify(classRepository).searchScoped(isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
	}

	@ParameterizedTest(name = "sortBy={0}, order={1}")
	@CsvSource({
			"classCode, asc, ASC",
			"' startDate ', DESC, DESC",
			"name, , ASC"
	})
	void listScopedSortsByRequestedWhitelistedField(String sortBy, String order, Sort.Direction expected) {
		givenEmptySearch();

		service.listScoped(principal(5L, RoleNames.CLASS_ADMIN), null, null, null, 0, 10, sortBy, order);

		verify(classRepository).searchScoped(
				eq(5L),
				isNull(),
				isNull(),
				isNull(),
				eq(PageRequest.of(0, 10, Sort.by(expected, sortBy.trim()))));
	}

	/** Only whitelisted columns may be sorted on, so a client cannot sort by an arbitrary entity path. */
	@ParameterizedTest(name = "sortBy={0} is rejected")
	@ValueSource(strings = {"trainingProgram.name", "deleted", "capacity"})
	void listScopedRejectsSortFieldOutsideWhitelist(String sortBy) {
		assertThatThrownBy(() -> service.listScoped(
				principal(5L, RoleNames.CLASS_ADMIN), null, null, null, 0, 10, sortBy, "asc"))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_SORT_FIELD");

		verify(classRepository, never()).searchScoped(any(), any(), any(), any(), any(Pageable.class));
	}

	@Test
	void listScopedRejectsUnknownSortOrder() {
		assertThatThrownBy(() -> service.listScoped(
				principal(5L, RoleNames.CLASS_ADMIN), null, null, null, 0, 10, "name", "sideways"))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_SORT_ORDER");

		verify(classRepository, never()).searchScoped(any(), any(), any(), any(), any(Pageable.class));
	}

	private TrainingProgram givenProgram(TrainingProgramStatus status) {
		TrainingProgram program = new TrainingProgram();
		program.setId(PROGRAM_ID);
		program.setName("Java Program");
		program.setStatus(status);
		when(trainingProgramRepository.findById(PROGRAM_ID)).thenReturn(Optional.of(program));
		return program;
	}

	/** An existing class with every field set, so tests can tell "kept" from "cleared". */
	private FapClass givenClass(ClassStatus status) {
		TrainingProgram program = new TrainingProgram();
		program.setId(PROGRAM_ID);
		program.setStatus(TrainingProgramStatus.Active);

		FapClass fapClass = new FapClass();
		fapClass.setId(CLASS_ID);
		fapClass.setName("Java Backend");
		fapClass.setClassCode("JV-01");
		fapClass.setTrainingProgram(program);
		fapClass.setStatus(status);
		fapClass.setLocation("Ha Noi");
		fapClass.setLocationDetail("Room 301");
		fapClass.setFsu("FHN");
		fapClass.setClassTime("Morning");
		fapClass.setStartDate(START);
		fapClass.setEndDate(END);
		fapClass.setDuration("11 weeks");
		fapClass.setCapacity(25);
		fapClass.setSelfEnrollmentEnabled(true);
		fapClass.setEnrollmentStartDate(ENROLLMENT_START);
		fapClass.setEnrollmentEndDate(ENROLLMENT_END);
		fapClass.setCreatedBy(1L);
		fapClass.setUpdatedBy(1L);
		when(classRepository.findWithTrainingProgramById(CLASS_ID)).thenReturn(Optional.of(fapClass));
		return fapClass;
	}

	private void givenEmptySearch() {
		when(classRepository.searchScoped(any(), any(), any(), any(), any(Pageable.class)))
				.thenAnswer(invocation -> Page.empty(invocation.getArgument(4)));
	}

	private static FapClass listedClass(long id) {
		FapClass fapClass = new FapClass();
		fapClass.setId(id);
		fapClass.setName("Class " + id);
		fapClass.setClassCode("C" + id);
		return fapClass;
	}

	private static CreateClassRequest createRequest(
			String classCode,
			LocalDate startDate,
			LocalDate endDate,
			LocalDate enrollmentStartDate,
			LocalDate enrollmentEndDate) {
		return new CreateClassRequest(
				"Java Backend",
				classCode,
				PROGRAM_ID,
				"Ha Noi",
				"Room 301",
				"FHN",
				"Morning",
				startDate,
				endDate,
				"11 weeks",
				25,
				true,
				enrollmentStartDate,
				enrollmentEndDate);
	}

	private static UpdateClassRequest emptyUpdateRequest() {
		return new UpdateClassRequest(null, null, null, null, null, null, null, null, null, null, null, null);
	}

	private static UpdateClassRequest updateRequestWithCapacity(int capacity) {
		return new UpdateClassRequest(null, null, null, null, null, null, null, null, capacity, null, null, null);
	}

	private static FapUserPrincipal principal(long id, String... roles) {
		return new FapUserPrincipal(id, "user" + id + "@fap.local", "", Set.of(roles), true, List.of());
	}

	/** Echoes the identifying fields only; the real mapping has its own collaborators and is not under test. */
	private static ClassResponse responseFor(FapClass fapClass) {
		return new ClassResponse(
				fapClass.getId(),
				fapClass.getName(),
				fapClass.getClassCode(),
				null,
				null,
				fapClass.getStatus(),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				0L,
				0L,
				false,
				null,
				null,
				null,
				null,
				null,
				null,
				null);
	}

	private FapClass captureSaved() {
		ArgumentCaptor<FapClass> captor = ArgumentCaptor.forClass(FapClass.class);
		verify(classRepository).save(captor.capture());
		return captor.getValue();
	}
}
