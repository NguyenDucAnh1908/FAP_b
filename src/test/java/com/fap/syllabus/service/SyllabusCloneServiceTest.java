package com.fap.syllabus.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.syllabus.dto.CloneSyllabusRequest;
import com.fap.syllabus.entity.MaterialFile;
import com.fap.syllabus.entity.MaterialFileContent;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.entity.SyllabusDay;
import com.fap.syllabus.entity.SyllabusOutputStandard;
import com.fap.syllabus.entity.SyllabusOutputStandardId;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyllabusCloneServiceTest {

	private static final long SOURCE_ID = 100L;
	private static final long CLONED_ID = 200L;
	private static final long SOURCE_MATERIAL_ID = 300L;
	private static final long CLONED_MATERIAL_ID = 400L;
	private static final long CURRENT_USER_ID = 9L;
	private static final byte[] FILE_DATA = {10, 20, 30};

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

	private Syllabus source;
	private MaterialFile sourceMaterial;
	private final AtomicReference<List<SyllabusDay>> savedDays = new AtomicReference<>();

	@BeforeEach
	void setUp() {
		source = sourceSyllabus(SyllabusStatus.Inactive);
		when(syllabusRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
		when(syllabusRepository.save(any(Syllabus.class))).thenAnswer(invocation -> {
			Syllabus saved = invocation.getArgument(0);
			saved.setId(CLONED_ID);
			return saved;
		});

		SyllabusOutputStandard standard = new SyllabusOutputStandard();
		standard.setSyllabus(source);
		standard.setId(new SyllabusOutputStandardId(SOURCE_ID, "H4SD"));
		when(outputStandardRepository.findByIdSyllabusIdOrderByIdStandardCodeAsc(SOURCE_ID))
				.thenReturn(List.of(standard));

		SyllabusDay day = new SyllabusDay();
		day.setSyllabus(source);
		day.setDayNumber(1);
		day.setSortOrder(1);
		SyllabusUnit unit = new SyllabusUnit();
		unit.setDay(day);
		unit.setName("Unit 1");
		unit.setSortOrder(1);
		SyllabusTopic topic = new SyllabusTopic();
		topic.setUnit(unit);
		topic.setName("Topic 1");
		topic.setOutputStandard("H4SD");
		topic.setOnline(true);
		topic.setDurationMinutes(30);
		topic.setStatus(SyllabusTopicStatus.Active);
		topic.setSortOrder(1);
		sourceMaterial = new MaterialFile();
		sourceMaterial.setId(SOURCE_MATERIAL_ID);
		sourceMaterial.setTopic(topic);
		sourceMaterial.setFileName("guide.pdf");
		sourceMaterial.setFileUrl(MaterialFileService.downloadPath(SOURCE_MATERIAL_ID));
		sourceMaterial.setFileSize((long) FILE_DATA.length);
		sourceMaterial.setContentType("application/pdf");
		sourceMaterial.setUploadedBy(7L);
		sourceMaterial.setUploadedAt(LocalDateTime.of(2026, 8, 12, 10, 0));
		topic.setMaterials(List.of(sourceMaterial));
		unit.setTopics(List.of(topic));
		day.setUnits(List.of(unit));
		when(dayRepository.findBySyllabusIdOrderBySortOrderAsc(SOURCE_ID)).thenReturn(List.of(day));

		doAnswer(invocation -> {
			Iterable<SyllabusDay> days = invocation.getArgument(0);
			List<SyllabusDay> captured = new java.util.ArrayList<>();
			days.forEach(captured::add);
			MaterialFile clonedMaterial = captured.getFirst().getUnits().getFirst()
					.getTopics().getFirst().getMaterials().getFirst();
			clonedMaterial.setId(CLONED_MATERIAL_ID);
			savedDays.set(captured);
			return captured;
		}).when(dayRepository).saveAll(any());
	}

	@ParameterizedTest
	@EnumSource(value = SyllabusStatus.class, names = {"Active", "Inactive"})
	void clonesPublishedSyllabusWithOutlineStandardsAndBlob(SyllabusStatus status) {
		source.setStatus(status);
		MaterialFileContent content = new MaterialFileContent();
		content.setMaterialFileId(SOURCE_MATERIAL_ID);
		content.setMaterialFile(sourceMaterial);
		content.setFileData(FILE_DATA);
		when(contentRepository.findAllById(Set.of(SOURCE_MATERIAL_ID))).thenReturn(List.of(content));

		service.cloneVersion(SOURCE_ID, request(), CURRENT_USER_ID);

		ArgumentCaptor<Syllabus> syllabusCaptor = ArgumentCaptor.forClass(Syllabus.class);
		verify(syllabusRepository).save(syllabusCaptor.capture());
		Syllabus cloned = syllabusCaptor.getValue();
		assertThat(cloned.getId()).isEqualTo(CLONED_ID);
		assertThat(cloned.getStatus()).isEqualTo(SyllabusStatus.Drafting);
		assertThat(cloned.getCode()).isEqualTo("HIS_SW_V2");
		assertThat(cloned.getVersion()).isEqualTo("v2.0");
		assertThat(cloned.getTechnicalRequirements()).isEqualTo(source.getTechnicalRequirements());
		assertThat(cloned.getCreatedBy()).isEqualTo(CURRENT_USER_ID);

		MaterialFile clonedMaterial = savedDays.get().getFirst().getUnits().getFirst()
				.getTopics().getFirst().getMaterials().getFirst();
		assertThat(clonedMaterial.getFileName()).isEqualTo("guide.pdf");
		assertThat(clonedMaterial.getFileUrl()).isEqualTo(MaterialFileService.downloadPath(CLONED_MATERIAL_ID));

		ArgumentCaptor<MaterialFileContent> contentCaptor = ArgumentCaptor.forClass(MaterialFileContent.class);
		verify(contentRepository).save(contentCaptor.capture());
		assertThat(contentCaptor.getValue().getFileData()).containsExactly(FILE_DATA);
		assertThat(contentCaptor.getValue().getMaterialFile()).isSameAs(clonedMaterial);
		verify(auditLogService).record("CLONE_SYLLABUS_VERSION:" + SOURCE_ID, "syllabus", CLONED_ID);
	}

	@Test
	void rejectsDuplicateCode() {
		when(syllabusRepository.existsByCodeIgnoreCase("his_sw_v2")).thenReturn(true);

		assertThatThrownBy(() -> service.cloneVersion(SOURCE_ID, request(), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("SYLLABUS_CODE_EXISTS");

		verify(syllabusRepository, never()).save(any());
	}

	@Test
	void keepsMissingInternalMaterialAsUnavailableForReupload() {
		when(contentRepository.findAllById(Set.of(SOURCE_MATERIAL_ID))).thenReturn(List.of());

		service.cloneVersion(SOURCE_ID, request(), CURRENT_USER_ID);

		MaterialFile clonedMaterial = savedDays.get().getFirst().getUnits().getFirst()
				.getTopics().getFirst().getMaterials().getFirst();
		assertThat(clonedMaterial.getFileUrl()).isEqualTo("unavailable");
		verify(contentRepository, never()).save(any());
	}

	@ParameterizedTest
	@EnumSource(value = SyllabusStatus.class, names = {"Drafting", "Pending"})
	void rejectsCloningEditableSyllabus(SyllabusStatus status) {
		source.setStatus(status);

		assertThatThrownBy(() -> service.cloneVersion(SOURCE_ID, request(), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("SYLLABUS_CLONE_NOT_ALLOWED");

		verify(syllabusRepository, never()).save(any());
	}

	private CloneSyllabusRequest request() {
		return new CloneSyllabusRequest("IS SOFTWARE", "his_sw_v2", "v2.0");
	}

	private Syllabus sourceSyllabus(SyllabusStatus status) {
		Syllabus syllabus = new Syllabus();
		syllabus.setId(SOURCE_ID);
		syllabus.setName("IS SOFTWARE");
		syllabus.setCode("HIS_SW_V1");
		syllabus.setVersion("v1.0");
		syllabus.setStatus(status);
		syllabus.setLevelName("Intermediate");
		syllabus.setAttendees(30);
		syllabus.setDuration("12 days");
		syllabus.setTechnicalRequirements("Java 21");
		syllabus.setCourseObjectives("Objectives");
		syllabus.setRules("Rules");
		syllabus.setTimeAllocAssignmentLab(50);
		syllabus.setTimeAllocConceptLecture(30);
		syllabus.setTimeAllocGuideReview(10);
		syllabus.setTimeAllocTestQuiz(10);
		syllabus.setAssessQuizPct(15);
		syllabus.setAssessAssignmentPct(15);
		syllabus.setAssessFinalPct(70);
		syllabus.setAssessmentText("Assessment");
		return syllabus;
	}
}
