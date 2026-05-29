package com.fap.syllabus.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.syllabus.dto.SyllabusImportResponse;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.mapper.SyllabusMapper;
import com.fap.syllabus.repository.SyllabusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyllabusImportServiceTest {

	private final SyllabusRepository syllabusRepository = mock(SyllabusRepository.class);
	private final SyllabusMapper syllabusMapper = mock(SyllabusMapper.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final SyllabusImportService service = new SyllabusImportService(
			syllabusRepository, syllabusMapper, auditLogService);

	@Test
	void importCsvRejectsEmptyFile() {
		MockMultipartFile file = new MockMultipartFile(
				"file", "test.csv", "text/csv", new byte[0]);

		assertThatThrownBy(() -> service.importCsv(file, 1L))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("empty");
	}

	@Test
	void importCsvRejectsNonCsvFile() {
		MockMultipartFile file = new MockMultipartFile(
				"file", "test.txt", "text/plain", "some content".getBytes());

		assertThatThrownBy(() -> service.importCsv(file, 1L))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("CSV");
	}

	@Test
	void importCsvRejectsMissingRequiredHeaders() {
		String csv = "name,wrong_column\nTest,Value";
		MockMultipartFile file = new MockMultipartFile(
				"file", "test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> service.importCsv(file, 1L))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("Missing required header");
	}

	@Test
	void importCsvImportsValidRows() {
		String csv = buildValidCsvWithOneRow("SYLLABUS001", "Test Syllabus 1");
		MockMultipartFile file = new MockMultipartFile(
				"file", "test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

		when(syllabusRepository.existsByCodeIgnoreCase("SYLLABUS001")).thenReturn(false);
		when(syllabusRepository.save(any(Syllabus.class))).thenAnswer(inv -> {
			Syllabus s = inv.getArgument(0);
			s.setId(1L);
			return s;
		});

		SyllabusImportResponse response = service.importCsv(file, 1L);

		assertThat(response.totalRows()).isEqualTo(1);
		assertThat(response.successCount()).isEqualTo(1);
		assertThat(response.failedCount()).isEqualTo(0);
		verify(syllabusRepository, times(1)).save(any(Syllabus.class));
		verify(auditLogService).record("IMPORT_SYLLABUS", "syllabus", 1L);
	}

	@Test
	void importCsvSkipsRowWithDuplicateCodeInDatabase() {
		String csv = buildValidCsvWithOneRow("EXISTING", "Existing Syllabus");
		MockMultipartFile file = new MockMultipartFile(
				"file", "test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

		when(syllabusRepository.existsByCodeIgnoreCase("EXISTING")).thenReturn(true);

		SyllabusImportResponse response = service.importCsv(file, 1L);

		assertThat(response.totalRows()).isEqualTo(1);
		assertThat(response.successCount()).isEqualTo(0);
		assertThat(response.failedCount()).isEqualTo(1);
		assertThat(response.errors()).hasSize(1);
		assertThat(response.errors().get(0).message()).contains("already exists");
		verify(syllabusRepository, never()).save(any(Syllabus.class));
	}

	@Test
	void importCsvSkipsRowWithMissingName() {
		String csv = buildCsvHeaders() + "\n,CODE001,v1.0,All levels,30,1 day,,,25,25,25,25,30,30,40,";
		MockMultipartFile file = new MockMultipartFile(
				"file", "test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

		SyllabusImportResponse response = service.importCsv(file, 1L);

		assertThat(response.failedCount()).isEqualTo(1);
		assertThat(response.errors().get(0).field()).isEqualTo("name");
	}

	@Test
	void importCsvSkipsRowWithInvalidTimeAllocation() {
		String csv = buildCsvHeaders() + "\nTest,CODE001,v1.0,All levels,30,1 day,,,10,10,10,10,30,30,40,";
		MockMultipartFile file = new MockMultipartFile(
				"file", "test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

		SyllabusImportResponse response = service.importCsv(file, 1L);

		assertThat(response.failedCount()).isEqualTo(1);
		assertThat(response.errors().get(0).field()).isEqualTo("time_allocation");
	}

	private String buildCsvHeaders() {
		return "name,code,version,level_name,attendees,duration,technical_requirements,course_objectives,rules," +
				"time_alloc_assignment_lab,time_alloc_concept_lecture,time_alloc_guide_review,time_alloc_test_quiz," +
				"assess_quiz_pct,assess_assignment_pct,assess_final_pct,assessment_text";
	}

	private String buildValidCsvWithOneRow(String code, String name) {
		return buildCsvHeaders() + "\n" +
				name + "," + code + ",v1.0,All levels,30,1 day,Tech requirements,Objectives,Rules," +
				"25,25,25,25,30,30,40,Assessment text";
	}
}

