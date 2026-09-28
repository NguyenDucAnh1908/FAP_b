package com.fap.syllabus.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.syllabus.dto.UpdateSyllabusOutputStandardsRequest;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.entity.SyllabusOutputStandard;
import com.fap.syllabus.entity.SyllabusOutputStandardId;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.repository.SyllabusOutputStandardRepository;
import com.fap.syllabus.repository.SyllabusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The output standards a syllabus covers are replaced as a whole set. Rows are keyed by
 * (syllabus id, standard code), so the old set must be deleted before the new one is inserted, and a
 * published syllabus keeps the standards it was approved with.
 */
class SyllabusOutputStandardServiceTest {

	private static final long SYLLABUS_ID = 10L;

	private final SyllabusRepository syllabusRepository = mock(SyllabusRepository.class);
	private final SyllabusOutputStandardRepository outputStandardRepository =
			mock(SyllabusOutputStandardRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);

	private final SyllabusOutputStandardService service = new SyllabusOutputStandardService(
			syllabusRepository,
			outputStandardRepository,
			auditLogService);

	@BeforeEach
	void useRealLookupDefaults() {
		lenient().doCallRealMethod().when(syllabusRepository).getOrThrow(any());
	}

	@Test
	void listRejectsUnknownSyllabus() {
		when(syllabusRepository.existsById(SYLLABUS_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.list(SYLLABUS_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Syllabus not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		verifyNoInteractions(outputStandardRepository);
	}

	@Test
	void listReturnsTheCodesInRepositoryOrder() {
		Syllabus syllabus = newSyllabus(SyllabusStatus.Active);
		when(syllabusRepository.existsById(SYLLABUS_ID)).thenReturn(true);
		when(outputStandardRepository.findByIdSyllabusIdOrderByIdStandardCodeAsc(SYLLABUS_ID)).thenReturn(List.of(
				SyllabusRules.createOutputStandard(syllabus, "C3SD"),
				SyllabusRules.createOutputStandard(syllabus, "H1SD")));

		assertThat(service.list(SYLLABUS_ID)).containsExactly("C3SD", "H1SD");
	}

	@ParameterizedTest(name = "standards of a {0} syllabus can be replaced")
	@EnumSource(value = SyllabusStatus.class, names = {"Drafting", "Pending"})
	void replaceStoresTheNewSetSortedByCode(SyllabusStatus status) {
		Syllabus syllabus = givenSyllabus(status);

		List<String> result = service.replace(SYLLABUS_ID, request("K6SD", "H1SD", "C3SD"));

		assertThat(result).containsExactly("C3SD", "H1SD", "K6SD");
		Iterable<SyllabusOutputStandard> saved = captureSaved();
		assertThat(saved).extracting(SyllabusOutputStandard::getId).containsExactly(
				new SyllabusOutputStandardId(SYLLABUS_ID, "C3SD"),
				new SyllabusOutputStandardId(SYLLABUS_ID, "H1SD"),
				new SyllabusOutputStandardId(SYLLABUS_ID, "K6SD"));
		assertThat(saved).allSatisfy(standard -> assertThat(standard.getSyllabus()).isSameAs(syllabus));
		verify(auditLogService).record("UPDATE_SYLLABUS_OUTPUT_STANDARDS", "syllabus", SYLLABUS_ID);
	}

	@Test
	void replaceDeletesTheOldSetBeforeSavingTheNewOne() {
		givenSyllabus(SyllabusStatus.Drafting);

		service.replace(SYLLABUS_ID, request("H1SD"));

		// Re-selecting a code that is already stored would violate the composite key otherwise.
		InOrder order = inOrder(outputStandardRepository, auditLogService);
		order.verify(outputStandardRepository).deleteByIdSyllabusId(SYLLABUS_ID);
		order.verify(outputStandardRepository).saveAll(anyIterable());
		order.verify(auditLogService).record("UPDATE_SYLLABUS_OUTPUT_STANDARDS", "syllabus", SYLLABUS_ID);
	}

	@ParameterizedTest(name = "standards of an {0} syllabus are frozen")
	@EnumSource(value = SyllabusStatus.class, names = {"Active", "Inactive"})
	void replaceRejectsPublishedSyllabus(SyllabusStatus status) {
		givenSyllabus(status);

		assertThatThrownBy(() -> service.replace(SYLLABUS_ID, request("H1SD")))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("SYLLABUS_NOT_EDITABLE");

		verifyNoInteractions(outputStandardRepository, auditLogService);
	}

	@Test
	void replaceRejectsUnknownSyllabus() {
		when(syllabusRepository.findById(SYLLABUS_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.replace(SYLLABUS_ID, request("H1SD")))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Syllabus not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		verifyNoInteractions(outputStandardRepository, auditLogService);
	}

	private Syllabus givenSyllabus(SyllabusStatus status) {
		Syllabus syllabus = newSyllabus(status);
		when(syllabusRepository.findById(SYLLABUS_ID)).thenReturn(Optional.of(syllabus));
		return syllabus;
	}

	private Iterable<SyllabusOutputStandard> captureSaved() {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Iterable<SyllabusOutputStandard>> saved = ArgumentCaptor.forClass(Iterable.class);
		verify(outputStandardRepository).saveAll(saved.capture());
		return saved.getValue();
	}

	private static UpdateSyllabusOutputStandardsRequest request(String... standards) {
		return new UpdateSyllabusOutputStandardsRequest(Set.of(standards));
	}

	private static Syllabus newSyllabus(SyllabusStatus status) {
		Syllabus syllabus = new Syllabus();
		syllabus.setId(SYLLABUS_ID);
		syllabus.setCode("JAVA-101");
		syllabus.setStatus(status);
		return syllabus;
	}
}
