package com.fap.syllabus.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.mapper.SyllabusMapper;
import com.fap.syllabus.mapper.SyllabusOutlineMapper;
import com.fap.syllabus.repository.SyllabusDayRepository;
import com.fap.syllabus.repository.SyllabusOutputStandardRepository;
import com.fap.syllabus.repository.SyllabusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the syllabus status lifecycle: Drafting -> Pending -> Active, with Inactive reachable
 * from Drafting and Pending. Every other move must be rejected.
 */
class SyllabusStateMachineTest {

	private static final long SYLLABUS_ID = 42L;
	private static final long CURRENT_USER_ID = 7L;

	private final SyllabusRepository syllabusRepository = mock(SyllabusRepository.class);
	private final SyllabusDayRepository dayRepository = mock(SyllabusDayRepository.class);
	private final SyllabusOutputStandardRepository outputStandardRepository =
			mock(SyllabusOutputStandardRepository.class);
	private final SyllabusMapper syllabusMapper = mock(SyllabusMapper.class);
	private final SyllabusOutlineMapper outlineMapper = mock(SyllabusOutlineMapper.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);

	private final SyllabusService service = new SyllabusService(
			syllabusRepository,
			dayRepository,
			outputStandardRepository,
			syllabusMapper,
			outlineMapper,
			auditLogService);

	@ParameterizedTest(name = "{0} -> {1} is allowed")
	@CsvSource({
			"Drafting, Pending",
			"Pending, Active",
			"Drafting, Inactive",
			"Pending, Inactive"
	})
	void allowsValidTransition(SyllabusStatus current, SyllabusStatus target) {
		Syllabus syllabus = givenSyllabus(current);
		givenOutlineAndOutputStandardsArePresent();

		service.updateStatus(SYLLABUS_ID, target, CURRENT_USER_ID);

		assertThat(syllabus.getStatus()).isEqualTo(target);
		assertThat(syllabus.getUpdatedBy()).isEqualTo(CURRENT_USER_ID);
		verify(auditLogService).record("UPDATE_SYLLABUS_STATUS:" + target.name(), "syllabus", SYLLABUS_ID);
	}

	@ParameterizedTest(name = "{0} -> {1} is rejected")
	@CsvSource({
			"Drafting, Active",
			"Pending, Drafting",
			"Active, Drafting",
			"Active, Pending",
			"Inactive, Drafting",
			"Inactive, Pending",
			"Inactive, Active"
	})
	void rejectsInvalidTransition(SyllabusStatus current, SyllabusStatus target) {
		Syllabus syllabus = givenSyllabus(current);
		givenOutlineAndOutputStandardsArePresent();

		assertThatThrownBy(() -> service.updateStatus(SYLLABUS_ID, target, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("INVALID_SYLLABUS_STATUS_TRANSITION");

		assertThat(syllabus.getStatus()).isEqualTo(current);
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@ParameterizedTest(name = "{0} -> {0} is a no-op")
	@CsvSource({"Drafting", "Pending", "Active", "Inactive"})
	void allowsTransitionToSameStatus(SyllabusStatus current) {
		Syllabus syllabus = givenSyllabus(current);
		givenOutlineAndOutputStandardsArePresent();

		service.updateStatus(SYLLABUS_ID, current, CURRENT_USER_ID);

		assertThat(syllabus.getStatus()).isEqualTo(current);
	}

	@Test
	void rejectsSubmissionWhenOutlineIsMissing() {
		givenSyllabus(SyllabusStatus.Drafting);
		when(syllabusRepository.countTopicsBySyllabusId(SYLLABUS_ID)).thenReturn(0L);

		assertThatThrownBy(() -> service.updateStatus(SYLLABUS_ID, SyllabusStatus.Pending, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("SYLLABUS_OUTLINE_REQUIRED");
	}

	@Test
	void rejectsSubmissionWhenOutputStandardsAreMissing() {
		givenSyllabus(SyllabusStatus.Drafting);
		when(syllabusRepository.countTopicsBySyllabusId(SYLLABUS_ID)).thenReturn(3L);
		when(syllabusRepository.countOutputStandardsBySyllabusId(SYLLABUS_ID)).thenReturn(0L);

		assertThatThrownBy(() -> service.updateStatus(SYLLABUS_ID, SyllabusStatus.Pending, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("SYLLABUS_OUTPUT_STANDARDS_REQUIRED");
	}

	@Test
	void rejectsSubmissionWhenTopicReferencesUnselectedOutputStandard() {
		givenSyllabus(SyllabusStatus.Drafting);
		when(syllabusRepository.countTopicsBySyllabusId(SYLLABUS_ID)).thenReturn(3L);
		when(syllabusRepository.countOutputStandardsBySyllabusId(SYLLABUS_ID)).thenReturn(2L);
		when(syllabusRepository.countTopicsWithoutSelectedOutputStandard(SYLLABUS_ID)).thenReturn(1L);

		assertThatThrownBy(() -> service.updateStatus(SYLLABUS_ID, SyllabusStatus.Pending, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("SYLLABUS_TOPIC_OUTPUT_STANDARD_INVALID");
	}

	@Test
	void publishingIsAlsoGuardedByOutlineCompleteness() {
		givenSyllabus(SyllabusStatus.Pending);
		when(syllabusRepository.countTopicsBySyllabusId(SYLLABUS_ID)).thenReturn(0L);

		assertThatThrownBy(() -> service.updateStatus(SYLLABUS_ID, SyllabusStatus.Active, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("SYLLABUS_OUTLINE_REQUIRED");
	}

	@Test
	void movingToInactiveSkipsOutlineCompletenessChecks() {
		Syllabus syllabus = givenSyllabus(SyllabusStatus.Drafting);
		when(syllabusRepository.countTopicsBySyllabusId(SYLLABUS_ID)).thenReturn(0L);

		service.updateStatus(SYLLABUS_ID, SyllabusStatus.Inactive, CURRENT_USER_ID);

		assertThat(syllabus.getStatus()).isEqualTo(SyllabusStatus.Inactive);
	}

	private Syllabus givenSyllabus(SyllabusStatus status) {
		Syllabus syllabus = new Syllabus();
		syllabus.setId(SYLLABUS_ID);
		syllabus.setStatus(status);
		when(syllabusRepository.findById(SYLLABUS_ID)).thenReturn(Optional.of(syllabus));
		return syllabus;
	}

	private void givenOutlineAndOutputStandardsArePresent() {
		when(syllabusRepository.countTopicsBySyllabusId(SYLLABUS_ID)).thenReturn(5L);
		when(syllabusRepository.countOutputStandardsBySyllabusId(SYLLABUS_ID)).thenReturn(3L);
		when(syllabusRepository.countTopicsWithoutSelectedOutputStandard(SYLLABUS_ID)).thenReturn(0L);
	}
}
