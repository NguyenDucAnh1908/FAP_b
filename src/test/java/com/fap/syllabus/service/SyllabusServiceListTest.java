package com.fap.syllabus.service;

import com.fap.common.audit.AuditLogService;
import com.fap.syllabus.dto.SyllabusResponse;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.mapper.SyllabusMapper;
import com.fap.syllabus.mapper.SyllabusOutlineMapper;
import com.fap.syllabus.repository.SyllabusDayRepository;
import com.fap.syllabus.repository.MaterialFileContentRepository;
import com.fap.syllabus.repository.SyllabusOutputStandardRepository;
import com.fap.syllabus.repository.SyllabusRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyllabusServiceListTest {

	private final SyllabusRepository syllabusRepository = mock(SyllabusRepository.class);
	private final SyllabusDayRepository dayRepository = mock(SyllabusDayRepository.class);
	private final MaterialFileContentRepository materialFileContentRepository =
			mock(MaterialFileContentRepository.class);
	private final SyllabusOutputStandardRepository outputStandardRepository =
			mock(SyllabusOutputStandardRepository.class);
	private final SyllabusMapper syllabusMapper = mock(SyllabusMapper.class);
	private final SyllabusOutlineMapper outlineMapper = mock(SyllabusOutlineMapper.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);

	private final SyllabusService service = new SyllabusService(
			syllabusRepository,
			dayRepository,
			materialFileContentRepository,
			outputStandardRepository,
			syllabusMapper,
			outlineMapper,
			auditLogService);

	private String captureLevelNamePassedToRepository(String inputLevelName) {
		when(syllabusRepository.search(any(), any(), any(), any())).thenReturn(Page.empty());

		service.list(SyllabusStatus.Active, inputLevelName, null, 0, 20);

		ArgumentCaptor<String> levelNameCaptor = ArgumentCaptor.forClass(String.class);
		verify(syllabusRepository).search(
				eq(SyllabusStatus.Active), levelNameCaptor.capture(), any(), any(Pageable.class));
		return levelNameCaptor.getValue();
	}

	@Test
	void listTreatsAllLevelsAsNoLevelFilter() {
		assertThat(captureLevelNamePassedToRepository("All levels")).isNull();
	}

	@Test
	void listTreatsAllAsNoLevelFilter() {
		assertThat(captureLevelNamePassedToRepository("all")).isNull();
	}

	@Test
	void listTreatsBlankLevelAsNoLevelFilter() {
		assertThat(captureLevelNamePassedToRepository("   ")).isNull();
	}

	@Test
	void listTreatsNullLevelAsNoLevelFilter() {
		assertThat(captureLevelNamePassedToRepository(null)).isNull();
	}

	@Test
	void listNormalizesConcreteLevelToTrimmedLowercase() {
		assertThat(captureLevelNamePassedToRepository("  Beginner  ")).isEqualTo("beginner");
	}
}
