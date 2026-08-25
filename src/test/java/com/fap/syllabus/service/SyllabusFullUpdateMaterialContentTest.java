package com.fap.syllabus.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.syllabus.dto.CreateFullSyllabusRequest;
import com.fap.syllabus.entity.MaterialFile;
import com.fap.syllabus.entity.MaterialFileContent;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.entity.SyllabusDay;
import com.fap.syllabus.entity.SyllabusTopic;
import com.fap.syllabus.entity.SyllabusUnit;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.enums.SyllabusTopicStatus;
import com.fap.syllabus.mapper.SyllabusMapper;
import com.fap.syllabus.mapper.SyllabusOutlineMapper;
import com.fap.syllabus.repository.MaterialFileContentRepository;
import com.fap.syllabus.repository.SyllabusDayRepository;
import com.fap.syllabus.repository.SyllabusOutputStandardRepository;
import com.fap.syllabus.repository.SyllabusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyllabusFullUpdateMaterialContentTest {

	private static final long SYLLABUS_ID = 100L;
	private static final long OLD_MATERIAL_ID = 135L;
	private static final long NEW_MATERIAL_ID = 200L;
	private static final byte[] FILE_DATA = {1, 2, 3, 4};

	private final SyllabusRepository syllabusRepository = mock(SyllabusRepository.class);
	private final SyllabusDayRepository dayRepository = mock(SyllabusDayRepository.class);
	private final MaterialFileContentRepository contentRepository = mock(MaterialFileContentRepository.class);
	private final SyllabusOutputStandardRepository outputStandardRepository =
			mock(SyllabusOutputStandardRepository.class);
	private final SyllabusMapper syllabusMapper = mock(SyllabusMapper.class);
	private final SyllabusOutlineMapper outlineMapper = mock(SyllabusOutlineMapper.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);

	private final SyllabusService service = new SyllabusService(
			syllabusRepository,
			dayRepository,
			contentRepository,
			outputStandardRepository,
			syllabusMapper,
			outlineMapper,
			auditLogService);

	private MaterialFile existingMaterial;

	@BeforeEach
	void setUp() {
		Syllabus syllabus = new Syllabus();
		syllabus.setId(SYLLABUS_ID);
		syllabus.setStatus(SyllabusStatus.Drafting);
		when(syllabusRepository.findById(SYLLABUS_ID)).thenReturn(Optional.of(syllabus));

		SyllabusDay day = new SyllabusDay();
		day.setSyllabus(syllabus);
		SyllabusUnit unit = new SyllabusUnit();
		unit.setDay(day);
		SyllabusTopic topic = new SyllabusTopic();
		topic.setUnit(unit);
		existingMaterial = new MaterialFile();
		existingMaterial.setId(OLD_MATERIAL_ID);
		existingMaterial.setTopic(topic);
		existingMaterial.setFileName("guide.pdf");
		existingMaterial.setFileUrl(MaterialFileService.downloadPath(OLD_MATERIAL_ID));
		existingMaterial.setFileSize((long) FILE_DATA.length);
		existingMaterial.setContentType("application/pdf");
		existingMaterial.setUploadedBy(7L);
		existingMaterial.setUploadedAt(LocalDateTime.of(2026, 8, 12, 10, 0));
		topic.setMaterials(List.of(existingMaterial));
		unit.setTopics(List.of(topic));
		day.setUnits(List.of(unit));
		when(dayRepository.findBySyllabusIdOrderBySortOrderAsc(SYLLABUS_ID)).thenReturn(List.of(day));

		doAnswer(invocation -> {
			Iterable<SyllabusDay> savedDays = invocation.getArgument(0);
			MaterialFile recreated = savedDays.iterator().next()
					.getUnits().getFirst().getTopics().getFirst().getMaterials().getFirst();
			recreated.setId(NEW_MATERIAL_ID);
			return savedDays;
		}).when(dayRepository).saveAll(any());
	}

	@Test
	void preservesUploadedBytesAndUpdatesDownloadUrl() {
		MaterialFileContent oldContent = new MaterialFileContent();
		oldContent.setMaterialFileId(OLD_MATERIAL_ID);
		oldContent.setMaterialFile(existingMaterial);
		oldContent.setFileData(FILE_DATA);
		when(contentRepository.findAllById(Set.of(OLD_MATERIAL_ID))).thenReturn(List.of(oldContent));

		service.updateFull(SYLLABUS_ID, request(), 99L);

		ArgumentCaptor<MaterialFileContent> captor = ArgumentCaptor.forClass(MaterialFileContent.class);
		verify(contentRepository).save(captor.capture());
		MaterialFileContent restored = captor.getValue();
		assertThat(restored.getMaterialFile().getId()).isEqualTo(NEW_MATERIAL_ID);
		assertThat(restored.getFileData()).containsExactly(FILE_DATA);
		assertThat(restored.getMaterialFile().getFileUrl())
				.isEqualTo(MaterialFileService.downloadPath(NEW_MATERIAL_ID));
		assertThat(restored.getMaterialFile().getUploadedBy()).isEqualTo(7L);
	}

	@Test
	void rejectsMissingBlobBeforeDeletingOutline() {
		when(contentRepository.findAllById(Set.of(OLD_MATERIAL_ID))).thenReturn(List.of());

		assertThatThrownBy(() -> service.updateFull(SYLLABUS_ID, request(), 99L))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("MATERIAL_CONTENT_MISSING");

		verify(dayRepository, never()).deleteBySyllabusId(SYLLABUS_ID);
	}

	private CreateFullSyllabusRequest request() {
		CreateFullSyllabusRequest.MaterialRequest material = new CreateFullSyllabusRequest.MaterialRequest(
				OLD_MATERIAL_ID,
				"guide.pdf",
				MaterialFileService.downloadPath(OLD_MATERIAL_ID),
				(long) FILE_DATA.length,
				"application/pdf");
		CreateFullSyllabusRequest.TopicRequest topic = new CreateFullSyllabusRequest.TopicRequest(
				"Topic 1", "H4SD", true, 30, SyllabusTopicStatus.Active, 1, List.of(material));
		CreateFullSyllabusRequest.UnitRequest unit = new CreateFullSyllabusRequest.UnitRequest(
				"Unit 1", 1, List.of(topic));
		CreateFullSyllabusRequest.DayRequest day = new CreateFullSyllabusRequest.DayRequest(1, 1, List.of(unit));
		return new CreateFullSyllabusRequest(
				"Syllabus",
				"SYL-01",
				"1.0",
				"Beginner",
				30,
				"1 day",
				"Java 21",
				"Objectives",
				"Rules",
				25,
				25,
				25,
				25,
				20,
				30,
				50,
				"Assessment",
				Set.of("H4SD"),
				List.of(day));
	}
}
