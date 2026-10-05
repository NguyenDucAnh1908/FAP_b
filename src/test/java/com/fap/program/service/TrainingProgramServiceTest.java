package com.fap.program.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.program.dto.CreateTrainingProgramRequest;
import com.fap.program.dto.TrainingProgramResponse;
import com.fap.program.dto.TrainingProgramSyllabusItemRequest;
import com.fap.program.dto.TrainingProgramSyllabusResponse;
import com.fap.program.dto.UpdateTrainingProgramRequest;
import com.fap.program.dto.UpdateTrainingProgramSyllabusesRequest;
import com.fap.program.entity.TrainingProgram;
import com.fap.program.entity.TrainingProgramSyllabus;
import com.fap.program.entity.TrainingProgramSyllabusId;
import com.fap.program.enums.TrainingProgramStatus;
import com.fap.program.mapper.TrainingProgramMapper;
import com.fap.program.repository.TrainingProgramRepository;
import com.fap.program.repository.TrainingProgramSyllabusRepository;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.repository.SyllabusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A training program is only editable while it is still Planning: once published, classes are
 * opened against its content, so its fields, its syllabus list and its existence are frozen. The
 * attached syllabuses must be distinct, uniquely ordered and already published (Active), because a
 * class runs exactly the syllabuses listed here, in this order. Lifecycle transitions are covered
 * by {@link TrainingProgramStateMachineTest}.
 */
class TrainingProgramServiceTest {

	private static final long PROGRAM_ID = 11L;
	private static final long CURRENT_USER_ID = 7L;
	private static final long CREATOR_ID = 3L;
	private static final long JAVA_SYLLABUS_ID = 101L;
	private static final long SQL_SYLLABUS_ID = 102L;

	private final TrainingProgramRepository programRepository = mock(TrainingProgramRepository.class);
	private final TrainingProgramSyllabusRepository programSyllabusRepository =
			mock(TrainingProgramSyllabusRepository.class);
	private final SyllabusRepository syllabusRepository = mock(SyllabusRepository.class);
	// The real mapper keeps the assertions on what the API returns, including the syllabus order.
	private final TrainingProgramMapper trainingProgramMapper = new TrainingProgramMapper();
	private final AuditLogService auditLogService = mock(AuditLogService.class);

	private final TrainingProgramService service = new TrainingProgramService(
			programRepository,
			programSyllabusRepository,
			syllabusRepository,
			trainingProgramMapper,
			auditLogService);

	@BeforeEach
	void delegateLookupsToFinders() {
		lenient().doCallRealMethod().when(programRepository).getTrainingProgramOrThrow(any());
		lenient().doCallRealMethod().when(syllabusRepository).getOrThrow(any());
	}

	@Test
	void listsNewestFirstByDefaultAndTreatsBlankKeywordAsNoFilter() {
		when(programRepository.search(any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(program(TrainingProgramStatus.Active))));

		Page<TrainingProgramResponse> page = service.list(TrainingProgramStatus.Active, "   ", 1, 10);

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(programRepository).search(eq(TrainingProgramStatus.Active), isNull(), pageable.capture());
		assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
		assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
		assertThat(page.getContent()).extracting(TrainingProgramResponse::id).containsExactly(PROGRAM_ID);
	}

	@Test
	void listPassesTrimmedKeywordAndWhitelistedSort() {
		when(programRepository.search(any(), any(), any())).thenReturn(Page.empty());

		service.list(null, "  java ", 0, 20, "totalHours", "desc");

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(programRepository).search(isNull(), eq("java"), pageable.capture());
		assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "totalHours"));
	}

	@Test
	void listRejectsSortFieldOutsideWhitelist() {
		assertThatThrownBy(() -> service.list(null, null, 0, 20, "deleted", "asc"))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_SORT_FIELD");

		verify(programRepository, never()).search(any(), any(), any());
	}

	@Test
	void createStartsProgramInPlanningWithAuditTrail() {
		when(programRepository.save(any())).thenAnswer(invocation -> {
			TrainingProgram program = invocation.getArgument(0);
			program.setId(PROGRAM_ID);
			return program;
		});
		LocalDateTime before = LocalDateTime.now();

		TrainingProgramResponse response = service.create(
				new CreateTrainingProgramRequest("Java Fresher", "12 weeks", 480, "v2.0"),
				CURRENT_USER_ID);

		LocalDateTime after = LocalDateTime.now();
		TrainingProgram saved = captureSavedProgram();
		assertThat(saved.getStatus()).isEqualTo(TrainingProgramStatus.Planning);
		assertThat(saved.getName()).isEqualTo("Java Fresher");
		assertThat(saved.getDuration()).isEqualTo("12 weeks");
		assertThat(saved.getTotalHours()).isEqualTo(480);
		assertThat(saved.getVersion()).isEqualTo("v2.0");
		assertThat(saved.getCreatedBy()).isEqualTo(CURRENT_USER_ID);
		assertThat(saved.getUpdatedBy()).isEqualTo(CURRENT_USER_ID);
		assertThat(saved.getCreatedAt()).isBetween(before, after);
		assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
		assertThat(saved.isDeleted()).isFalse();
		assertThat(response.id()).isEqualTo(PROGRAM_ID);
		assertThat(response.status()).isEqualTo(TrainingProgramStatus.Planning);
		verify(auditLogService).record("CREATE_TRAINING_PROGRAM", "training_program", PROGRAM_ID);
	}

	@Test
	void getReturnsProgram() {
		givenProgram(TrainingProgramStatus.Active);

		TrainingProgramResponse response = service.get(PROGRAM_ID);

		assertThat(response.id()).isEqualTo(PROGRAM_ID);
		assertThat(response.status()).isEqualTo(TrainingProgramStatus.Active);
	}

	@Test
	void getRejectsUnknownProgram() {
		givenMissingProgram();

		assertThatThrownBy(() -> service.get(PROGRAM_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Training program not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");
	}

	@Test
	void updateReplacesFieldsOfPlanningProgram() {
		TrainingProgram program = givenProgram(TrainingProgramStatus.Planning);
		LocalDateTime createdAt = program.getCreatedAt();
		LocalDateTime before = LocalDateTime.now();

		TrainingProgramResponse response = service.update(
				PROGRAM_ID,
				new UpdateTrainingProgramRequest("Java Fresher 2", "10 weeks", 400, "v3.0"),
				CURRENT_USER_ID);

		assertThat(program.getName()).isEqualTo("Java Fresher 2");
		assertThat(program.getDuration()).isEqualTo("10 weeks");
		assertThat(program.getTotalHours()).isEqualTo(400);
		assertThat(program.getVersion()).isEqualTo("v3.0");
		assertThat(program.getStatus()).isEqualTo(TrainingProgramStatus.Planning);
		assertThat(program.getUpdatedBy()).isEqualTo(CURRENT_USER_ID);
		assertThat(program.getUpdatedAt()).isAfterOrEqualTo(before);
		assertThat(program.getCreatedBy()).isEqualTo(CREATOR_ID);
		assertThat(program.getCreatedAt()).isEqualTo(createdAt);
		assertThat(response.name()).isEqualTo("Java Fresher 2");
		verify(auditLogService).record("UPDATE_TRAINING_PROGRAM", "training_program", PROGRAM_ID);
	}

	@ParameterizedTest(name = "update of a {0} program is rejected")
	@EnumSource(value = TrainingProgramStatus.class, names = "Planning", mode = EnumSource.Mode.EXCLUDE)
	void updateRejectsProgramThatIsNoLongerPlanning(TrainingProgramStatus status) {
		TrainingProgram program = givenProgram(status);

		assertThatThrownBy(() -> service.update(
				PROGRAM_ID,
				new UpdateTrainingProgramRequest("Renamed", "1 week", 1, "v9.9"),
				CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("TRAINING_PROGRAM_NOT_EDITABLE");

		assertThat(program.getName()).isEqualTo("Java Fresher");
		assertThat(program.getUpdatedBy()).isEqualTo(CREATOR_ID);
		verifyNoAudit();
	}

	@Test
	void updateRejectsUnknownProgram() {
		givenMissingProgram();

		assertThatThrownBy(() -> service.update(
				PROGRAM_ID,
				new UpdateTrainingProgramRequest("Renamed", "1 week", 1, "v9.9"),
				CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Training program not found");

		verifyNoAudit();
	}

	@Test
	void deleteSoftDeletesPlanningProgram() {
		TrainingProgram program = givenProgram(TrainingProgramStatus.Planning);
		LocalDateTime before = LocalDateTime.now();

		service.delete(PROGRAM_ID, CURRENT_USER_ID);

		assertThat(program.isDeleted()).isTrue();
		assertThat(program.getDeletedAt()).isAfterOrEqualTo(before);
		assertThat(program.getUpdatedBy()).isEqualTo(CURRENT_USER_ID);
		verify(programRepository, never()).delete(any());
		verify(programRepository, never()).deleteById(any());
		verify(auditLogService).record("DELETE_TRAINING_PROGRAM", "training_program", PROGRAM_ID);
	}

	@ParameterizedTest(name = "delete of a {0} program is rejected")
	@EnumSource(value = TrainingProgramStatus.class, names = "Planning", mode = EnumSource.Mode.EXCLUDE)
	void deleteRejectsProgramThatIsNoLongerPlanning(TrainingProgramStatus status) {
		TrainingProgram program = givenProgram(status);

		assertThatThrownBy(() -> service.delete(PROGRAM_ID, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("TRAINING_PROGRAM_NOT_EDITABLE");

		assertThat(program.isDeleted()).isFalse();
		assertThat(program.getDeletedAt()).isNull();
		verifyNoAudit();
	}

	@Test
	void listSyllabusesReturnsAttachedSyllabusesInStoredOrder() {
		when(programRepository.existsById(PROGRAM_ID)).thenReturn(true);
		TrainingProgram program = program(TrainingProgramStatus.Active);
		when(programSyllabusRepository.findByIdProgramIdOrderBySortOrderAsc(PROGRAM_ID)).thenReturn(List.of(
				attachment(program, syllabus(SQL_SYLLABUS_ID, SyllabusStatus.Active), 1),
				attachment(program, syllabus(JAVA_SYLLABUS_ID, SyllabusStatus.Active), 2)));

		List<TrainingProgramSyllabusResponse> syllabuses = service.listSyllabuses(PROGRAM_ID);

		assertThat(syllabuses)
				.extracting(TrainingProgramSyllabusResponse::syllabusId, TrainingProgramSyllabusResponse::sortOrder)
				.containsExactly(
						tuple(SQL_SYLLABUS_ID, 1),
						tuple(JAVA_SYLLABUS_ID, 2));
	}

	@Test
	void listSyllabusesRejectsUnknownProgram() {
		when(programRepository.existsById(PROGRAM_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.listSyllabuses(PROGRAM_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Training program not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		verify(programSyllabusRepository, never()).findByIdProgramIdOrderBySortOrderAsc(anyLong());
	}

	/**
	 * The old list is removed before the new rows are inserted: both share the composite key
	 * (program, syllabus), so inserting first would collide with a syllabus that is kept.
	 */
	@Test
	void replaceSyllabusesSwapsTheWholeListAndReturnsItSortedBySortOrder() {
		TrainingProgram program = givenProgram(TrainingProgramStatus.Planning);
		Syllabus java = givenSyllabus(JAVA_SYLLABUS_ID, SyllabusStatus.Active);
		Syllabus sql = givenSyllabus(SQL_SYLLABUS_ID, SyllabusStatus.Active);
		LocalDateTime before = LocalDateTime.now();

		List<TrainingProgramSyllabusResponse> response = service.replaceSyllabuses(
				PROGRAM_ID,
				request(item(JAVA_SYLLABUS_ID, 2), item(SQL_SYLLABUS_ID, 1)),
				CURRENT_USER_ID);

		InOrder order = inOrder(programSyllabusRepository);
		order.verify(programSyllabusRepository).deleteByIdProgramId(PROGRAM_ID);
		ArgumentCaptor<List<TrainingProgramSyllabus>> saved = ArgumentCaptor.captor();
		order.verify(programSyllabusRepository).saveAll(saved.capture());
		assertThat(saved.getValue())
				.extracting(TrainingProgramSyllabus::getSyllabus, TrainingProgramSyllabus::getSortOrder)
				.containsExactly(
						tuple(java, 2),
						tuple(sql, 1));
		assertThat(saved.getValue()).allSatisfy(item -> assertThat(item.getProgram()).isSameAs(program));
		assertThat(saved.getValue())
				.extracting(TrainingProgramSyllabus::getId)
				.containsExactly(
						new TrainingProgramSyllabusId(PROGRAM_ID, JAVA_SYLLABUS_ID),
						new TrainingProgramSyllabusId(PROGRAM_ID, SQL_SYLLABUS_ID));
		assertThat(response)
				.extracting(TrainingProgramSyllabusResponse::syllabusId, TrainingProgramSyllabusResponse::sortOrder)
				.containsExactly(
						tuple(SQL_SYLLABUS_ID, 1),
						tuple(JAVA_SYLLABUS_ID, 2));
		assertThat(program.getUpdatedBy()).isEqualTo(CURRENT_USER_ID);
		assertThat(program.getUpdatedAt()).isAfterOrEqualTo(before);
		verify(auditLogService).record("UPDATE_TRAINING_PROGRAM_SYLLABUSES", "training_program", PROGRAM_ID);
	}

	@Test
	void replaceSyllabusesRejectsTheSameSyllabusTwice() {
		givenProgram(TrainingProgramStatus.Planning);

		assertThatThrownBy(() -> service.replaceSyllabuses(
				PROGRAM_ID,
				request(item(JAVA_SYLLABUS_ID, 1), item(JAVA_SYLLABUS_ID, 2)),
				CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("DUPLICATE_TRAINING_PROGRAM_SYLLABUS");

		verifyListUntouched();
	}

	@Test
	void replaceSyllabusesRejectsDuplicateSortOrder() {
		givenProgram(TrainingProgramStatus.Planning);

		assertThatThrownBy(() -> service.replaceSyllabuses(
				PROGRAM_ID,
				request(item(JAVA_SYLLABUS_ID, 1), item(SQL_SYLLABUS_ID, 1)),
				CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("DUPLICATE_TRAINING_PROGRAM_SYLLABUS_SORT_ORDER");

		verifyListUntouched();
	}

	/** Only published syllabuses may be taught, so a draft or retired one cannot be attached. */
	@ParameterizedTest(name = "a {0} syllabus cannot be attached")
	@EnumSource(value = SyllabusStatus.class, names = "Active", mode = EnumSource.Mode.EXCLUDE)
	void replaceSyllabusesRejectsSyllabusThatIsNotActive(SyllabusStatus status) {
		givenProgram(TrainingProgramStatus.Planning);
		givenSyllabus(JAVA_SYLLABUS_ID, SyllabusStatus.Active);
		givenSyllabus(SQL_SYLLABUS_ID, status);

		assertThatThrownBy(() -> service.replaceSyllabuses(
				PROGRAM_ID,
				request(item(JAVA_SYLLABUS_ID, 1), item(SQL_SYLLABUS_ID, 2)),
				CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("TRAINING_PROGRAM_SYLLABUS_NOT_ACTIVE");

		verify(programSyllabusRepository, never()).saveAll(any());
		verifyNoAudit();
	}

	@Test
	void replaceSyllabusesRejectsUnknownSyllabus() {
		givenProgram(TrainingProgramStatus.Planning);
		when(syllabusRepository.findById(JAVA_SYLLABUS_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.replaceSyllabuses(
				PROGRAM_ID,
				request(item(JAVA_SYLLABUS_ID, 1)),
				CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Syllabus not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		verify(programSyllabusRepository, never()).saveAll(any());
		verifyNoAudit();
	}

	@ParameterizedTest(name = "syllabuses of a {0} program are frozen")
	@EnumSource(value = TrainingProgramStatus.class, names = "Planning", mode = EnumSource.Mode.EXCLUDE)
	void replaceSyllabusesRejectsProgramThatIsNoLongerPlanning(TrainingProgramStatus status) {
		givenProgram(status);

		assertThatThrownBy(() -> service.replaceSyllabuses(
				PROGRAM_ID,
				request(item(JAVA_SYLLABUS_ID, 1)),
				CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("TRAINING_PROGRAM_NOT_EDITABLE");

		verifyListUntouched();
	}

	@Test
	void replaceSyllabusesRejectsUnknownProgram() {
		givenMissingProgram();

		assertThatThrownBy(() -> service.replaceSyllabuses(
				PROGRAM_ID,
				request(item(JAVA_SYLLABUS_ID, 1)),
				CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Training program not found");

		verifyListUntouched();
	}

	private TrainingProgram givenProgram(TrainingProgramStatus status) {
		TrainingProgram program = program(status);
		when(programRepository.findById(PROGRAM_ID)).thenReturn(Optional.of(program));
		return program;
	}

	private void givenMissingProgram() {
		when(programRepository.findById(PROGRAM_ID)).thenReturn(Optional.empty());
	}

	private Syllabus givenSyllabus(long id, SyllabusStatus status) {
		Syllabus syllabus = syllabus(id, status);
		when(syllabusRepository.findById(id)).thenReturn(Optional.of(syllabus));
		return syllabus;
	}

	private TrainingProgram captureSavedProgram() {
		ArgumentCaptor<TrainingProgram> saved = ArgumentCaptor.forClass(TrainingProgram.class);
		verify(programRepository).save(saved.capture());
		return saved.getValue();
	}

	/** Rejected edits must leave the attached list and the audit log as they were. */
	private void verifyListUntouched() {
		verify(programSyllabusRepository, never()).deleteByIdProgramId(anyLong());
		verify(programSyllabusRepository, never()).saveAll(any());
		verifyNoInteractions(syllabusRepository);
		verifyNoAudit();
	}

	private void verifyNoAudit() {
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	private static TrainingProgram program(TrainingProgramStatus status) {
		TrainingProgram program = new TrainingProgram();
		program.setId(PROGRAM_ID);
		program.setName("Java Fresher");
		program.setDuration("12 weeks");
		program.setTotalHours(480);
		program.setVersion("v2.0");
		program.setStatus(status);
		program.setCreatedBy(CREATOR_ID);
		program.setUpdatedBy(CREATOR_ID);
		program.setCreatedAt(LocalDateTime.now().minusDays(3));
		program.setUpdatedAt(LocalDateTime.now().minusDays(3));
		return program;
	}

	private static Syllabus syllabus(long id, SyllabusStatus status) {
		Syllabus syllabus = new Syllabus();
		syllabus.setId(id);
		syllabus.setCode("SYL-" + id);
		syllabus.setName("Syllabus " + id);
		syllabus.setStatus(status);
		return syllabus;
	}

	private static TrainingProgramSyllabus attachment(TrainingProgram program, Syllabus syllabus, int sortOrder) {
		TrainingProgramSyllabus attachment = new TrainingProgramSyllabus();
		attachment.setId(new TrainingProgramSyllabusId(program.getId(), syllabus.getId()));
		attachment.setProgram(program);
		attachment.setSyllabus(syllabus);
		attachment.setSortOrder(sortOrder);
		return attachment;
	}

	private static TrainingProgramSyllabusItemRequest item(long syllabusId, int sortOrder) {
		return new TrainingProgramSyllabusItemRequest(syllabusId, sortOrder);
	}

	private static UpdateTrainingProgramSyllabusesRequest request(TrainingProgramSyllabusItemRequest... items) {
		return new UpdateTrainingProgramSyllabusesRequest(List.of(items));
	}
}
