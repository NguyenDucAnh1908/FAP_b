package com.fap.syllabus.service;

import com.fap.clazz.enums.ClassEnrollmentStatus;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.metrics.DomainMetrics;
import com.fap.common.util.FileValidator;
import com.fap.syllabus.dto.AssignedMaterialFileResponse;
import com.fap.syllabus.dto.CreateMaterialFileRequest;
import com.fap.syllabus.dto.CreateMaterialRequest;
import com.fap.syllabus.dto.MaterialFileResponse;
import com.fap.syllabus.dto.UpdateMaterialFileRequest;
import com.fap.syllabus.entity.MaterialFile;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.entity.SyllabusDay;
import com.fap.syllabus.entity.SyllabusTopic;
import com.fap.syllabus.entity.SyllabusUnit;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.mapper.MaterialFileMapper;
import com.fap.syllabus.repository.MaterialFileContentRepository;
import com.fap.syllabus.repository.MaterialFileRepository;
import com.fap.syllabus.repository.SyllabusRepository;
import com.fap.syllabus.repository.SyllabusTopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Guards how materials are attached to a syllabus topic: links and uploads may only change while the
 * syllabus is still Drafting or Pending, every lookup is scoped so one syllabus cannot touch another's
 * materials, and an upload is counted exactly once as a success or a failure for monitoring.
 * Downloads are covered by {@link MaterialDownloadAuthorizationTest}.
 */
class MaterialFileServiceTest {

	private static final long SYLLABUS_ID = 10L;
	private static final long OTHER_SYLLABUS_ID = 11L;
	private static final long TOPIC_ID = 40L;
	private static final long OTHER_TOPIC_ID = 49L;
	private static final long MATERIAL_ID = 41L;
	private static final long UPLOADER_ID = 7L;
	private static final long TRAINEE_ID = 8L;
	private static final LocalDateTime ORIGINAL_UPLOADED_AT = LocalDateTime.of(2026, 3, 2, 8, 15);
	private static final byte[] CONTENT = "week one slides".getBytes(StandardCharsets.UTF_8);
	private static final String EXTERNAL_URL = "https://cdn.example.com/week-1.pdf";
	private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "uploadedAt", "id");

	private final SyllabusRepository syllabusRepository = mock(SyllabusRepository.class);
	private final SyllabusTopicRepository topicRepository = mock(SyllabusTopicRepository.class);
	private final MaterialFileRepository materialFileRepository = mock(MaterialFileRepository.class);
	private final MaterialFileContentRepository materialFileContentRepository =
			mock(MaterialFileContentRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final DomainMetrics domainMetrics = mock(DomainMetrics.class);

	private final MaterialFileService service = new MaterialFileService(
			syllabusRepository,
			topicRepository,
			materialFileRepository,
			materialFileContentRepository,
			new MaterialFileMapper(),
			new FileValidator(),
			auditLogService,
			domainMetrics);

	/** The file URL a material had at the moment it was first saved, before any later mutation. */
	private String fileUrlWhenSaved;

	@BeforeEach
	void useRealLookupDefaults() {
		lenient().doCallRealMethod().when(syllabusRepository).getOrThrow(any());
		lenient().doCallRealMethod().when(topicRepository).getByIdAndUnitDaySyllabusIdOrThrow(any(), any());
		lenient().doCallRealMethod().when(materialFileRepository).getWithTopicOrThrow(any());
		lenient().doCallRealMethod().when(materialFileRepository).getByIdAndTopicIdOrThrow(any(), any());
	}

	@BeforeEach
	void saveAssignsTheGeneratedId() {
		lenient().when(materialFileRepository.save(any(MaterialFile.class))).thenAnswer(invocation -> {
			MaterialFile material = invocation.getArgument(0);
			fileUrlWhenSaved = material.getFileUrl();
			material.setId(MATERIAL_ID);
			return material;
		});
	}

	@Test
	void uploadStreamsTheBytesAndPointsTheUrlAtTheDownloadEndpoint() {
		SyllabusTopic topic = givenTopicOf(givenSyllabus(SyllabusStatus.Drafting));
		AtomicReference<byte[]> streamed = new AtomicReference<>();
		doAnswer(invocation -> {
			InputStream data = invocation.getArgument(1);
			streamed.set(data.readAllBytes());
			return null;
		}).when(materialFileContentRepository).insertContent(eq(MATERIAL_ID), any(InputStream.class), anyLong());
		LocalDateTime before = LocalDateTime.now();

		MaterialFileResponse response = service.upload(SYLLABUS_ID, TOPIC_ID, pdf("week-1.pdf"), UPLOADER_ID);

		assertThat(response.id()).isEqualTo(MATERIAL_ID);
		assertThat(response.topicId()).isEqualTo(topic.getId());
		assertThat(response.fileName()).isEqualTo("week-1.pdf");
		assertThat(response.fileUrl()).isEqualTo("/api/v1/materials/41/download");
		assertThat(response.fileSize()).isEqualTo((long) CONTENT.length);
		assertThat(response.contentType()).isEqualTo("application/pdf");
		assertThat(response.uploadedBy()).isEqualTo(UPLOADER_ID);
		assertThat(response.uploadedAt()).isBetween(before, LocalDateTime.now());
		assertThat(response.storedContent()).isTrue();
		assertThat(streamed.get()).isEqualTo(CONTENT);
		verify(materialFileContentRepository).insertContent(
				eq(MATERIAL_ID), any(InputStream.class), eq((long) CONTENT.length));
		verify(auditLogService).record("UPLOAD_MATERIAL_FILE", "syllabus", SYLLABUS_ID);
		verify(domainMetrics).recordUpload(true);
		verify(domainMetrics, never()).recordUpload(false);
	}

	@Test
	void uploadSavesAPlaceholderUrlAndFlushesTheRowBeforeStreamingContent() {
		givenTopicOf(givenSyllabus(SyllabusStatus.Pending));

		service.upload(SYLLABUS_ID, TOPIC_ID, pdf("week-1.pdf"), UPLOADER_ID);

		// file_url is NOT NULL but the real URL needs the generated id, and the content row has a
		// foreign key to material_files, so the row must be saved and flushed before any bytes move.
		assertThat(fileUrlWhenSaved).isEqualTo(MaterialFileService.PENDING_FILE_URL);
		InOrder order = inOrder(materialFileRepository, materialFileContentRepository);
		order.verify(materialFileRepository).save(any(MaterialFile.class));
		order.verify(materialFileRepository).flush();
		order.verify(materialFileContentRepository).insertContent(eq(MATERIAL_ID), any(InputStream.class), anyLong());
	}

	@Test
	void uploadKeepsOnlyTheBareClientFileName() {
		givenTopicOf(givenSyllabus(SyllabusStatus.Drafting));

		MaterialFileResponse response = service.upload(
				SYLLABUS_ID, TOPIC_ID, pdf("..\\..\\slides\\week 1?.pdf"), UPLOADER_ID);

		assertThat(response.fileName()).isEqualTo("week 1_.pdf");
	}

	@ParameterizedTest(name = "upload to an {0} syllabus is rejected")
	@EnumSource(value = SyllabusStatus.class, names = {"Active", "Inactive"})
	void uploadRejectsPublishedSyllabus(SyllabusStatus status) {
		givenTopicOf(givenSyllabus(status));

		assertThatThrownBy(() -> service.upload(SYLLABUS_ID, TOPIC_ID, pdf("week-1.pdf"), UPLOADER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("SYLLABUS_NOT_EDITABLE");

		verifyNothingStored();
		verifyUploadCountedOnceAsFailure();
	}

	@Test
	void uploadRejectsUnknownSyllabus() {
		when(syllabusRepository.findById(SYLLABUS_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.upload(SYLLABUS_ID, TOPIC_ID, pdf("week-1.pdf"), UPLOADER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Syllabus not found");

		verifyNothingStored();
		verifyUploadCountedOnceAsFailure();
	}

	@Test
	void uploadRejectsTopicOfAnotherSyllabus() {
		givenSyllabus(SyllabusStatus.Drafting);
		givenTopicOfAnotherSyllabus();

		assertThatThrownBy(() -> service.upload(SYLLABUS_ID, TOPIC_ID, pdf("week-1.pdf"), UPLOADER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Syllabus topic not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		verifyNothingStored();
		verifyUploadCountedOnceAsFailure();
	}

	@ParameterizedTest(name = "[{0}] with {2} is rejected as {3}")
	@CsvSource({
			"week-1.pdf, '', application/pdf, FILE_REQUIRED",
			"setup.exe, MZ, application/x-msdownload, FILE_TYPE_NOT_ALLOWED",
			"week-1.pdf, slides, , FILE_TYPE_NOT_ALLOWED",
			"'', slides, application/pdf, FILE_NAME_REQUIRED",
			"'..', slides, application/pdf, FILE_NAME_INVALID"
	})
	void uploadRejectsInvalidFile(String fileName, String content, String contentType, String expectedCode) {
		givenTopicOf(givenSyllabus(SyllabusStatus.Drafting));
		MultipartFile file = new MockMultipartFile(
				"file", fileName, contentType, content.getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> service.upload(SYLLABUS_ID, TOPIC_ID, file, UPLOADER_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo(expectedCode);

		verifyNothingStored();
		verifyUploadCountedOnceAsFailure();
	}

	@Test
	void uploadOfAnUnreadableFileIsCountedOnceAsFailure() {
		givenTopicOf(givenSyllabus(SyllabusStatus.Drafting));
		MultipartFile unreadable = new MockMultipartFile("file", "week-1.pdf", "application/pdf", CONTENT) {
			@Override
			public InputStream getInputStream() throws IOException {
				throw new IOException("multipart temp file was removed");
			}
		};

		assertThatThrownBy(() -> service.upload(SYLLABUS_ID, TOPIC_ID, unreadable, UPLOADER_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("FILE_UNREADABLE");

		// This path counts the failure itself before rethrowing; the finally block must not count it again.
		verifyUploadCountedOnceAsFailure();
		verify(materialFileContentRepository, never()).insertContent(anyLong(), any(), anyLong());
		verifyNoInteractions(auditLogService);
	}

	@Test
	void uploadWhoseContentCannotBeStoredIsCountedOnceAsFailure() {
		givenTopicOf(givenSyllabus(SyllabusStatus.Drafting));
		doThrow(new IllegalStateException("tablespace full"))
				.when(materialFileContentRepository).insertContent(anyLong(), any(), anyLong());

		assertThatThrownBy(() -> service.upload(SYLLABUS_ID, TOPIC_ID, pdf("week-1.pdf"), UPLOADER_ID))
				.isInstanceOf(IllegalStateException.class);

		verifyUploadCountedOnceAsFailure();
		verifyNoInteractions(auditLogService);
	}

	@Test
	void createAttachesATrimmedLinkToTheTopic() {
		SyllabusTopic topic = givenTopicOf(givenSyllabus(SyllabusStatus.Drafting));
		LocalDateTime before = LocalDateTime.now();

		MaterialFileResponse response = service.create(SYLLABUS_ID, TOPIC_ID, new CreateMaterialFileRequest(
				"  week-1.pdf ", " " + EXTERNAL_URL + " ", 2048L, " application/pdf "), UPLOADER_ID);

		MaterialFile saved = captureSaved();
		assertThat(saved.getTopic()).isSameAs(topic);
		assertThat(saved.getFileName()).isEqualTo("week-1.pdf");
		assertThat(saved.getFileUrl()).isEqualTo(EXTERNAL_URL);
		assertThat(saved.getFileSize()).isEqualTo(2048L);
		assertThat(saved.getContentType()).isEqualTo("application/pdf");
		assertThat(saved.getUploadedBy()).isEqualTo(UPLOADER_ID);
		assertThat(saved.getUploadedAt()).isBetween(before, LocalDateTime.now());
		assertThat(response.id()).isEqualTo(MATERIAL_ID);
		assertThat(response.storedContent()).isFalse();
		verify(auditLogService).record("CREATE_MATERIAL_FILE", "syllabus", SYLLABUS_ID);
		// A link has no bytes of its own and is not an upload.
		verifyNoInteractions(materialFileContentRepository, domainMetrics);
	}

	@ParameterizedTest(name = "content type [{0}] is stored as [{1}]")
	@CsvSource({
			"application/pdf, application/pdf",
			"'  text/plain ', text/plain",
			"'   ', ",
			", "
	})
	void createStoresABlankContentTypeAsNull(String contentType, String expected) {
		givenTopicOf(givenSyllabus(SyllabusStatus.Pending));

		service.create(SYLLABUS_ID, TOPIC_ID, new CreateMaterialFileRequest(
				"week-1.pdf", EXTERNAL_URL, 2048L, contentType), UPLOADER_ID);

		assertThat(captureSaved().getContentType()).isEqualTo(expected);
	}

	@Test
	void createFromTheFlatRequestUsesItsSyllabusAndTopic() {
		SyllabusTopic topic = givenTopicOf(givenSyllabus(SyllabusStatus.Drafting));

		service.create(new CreateMaterialRequest(
				SYLLABUS_ID, TOPIC_ID, "week-1.pdf", EXTERNAL_URL, 2048L, "application/pdf"), UPLOADER_ID);

		MaterialFile saved = captureSaved();
		assertThat(saved.getTopic()).isSameAs(topic);
		assertThat(saved.getFileUrl()).isEqualTo(EXTERNAL_URL);
		assertThat(saved.getUploadedBy()).isEqualTo(UPLOADER_ID);
		verify(auditLogService).record("CREATE_MATERIAL_FILE", "syllabus", SYLLABUS_ID);
	}

	@ParameterizedTest(name = "a link cannot be added to an {0} syllabus")
	@EnumSource(value = SyllabusStatus.class, names = {"Active", "Inactive"})
	void createRejectsPublishedSyllabus(SyllabusStatus status) {
		givenTopicOf(givenSyllabus(status));

		assertThatThrownBy(() -> service.create(SYLLABUS_ID, TOPIC_ID, linkRequest(EXTERNAL_URL), UPLOADER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("SYLLABUS_NOT_EDITABLE");

		verifyNoInteractions(topicRepository);
		verifyNothingStored();
	}

	@Test
	void createRejectsTopicOfAnotherSyllabus() {
		givenSyllabus(SyllabusStatus.Drafting);
		givenTopicOfAnotherSyllabus();

		assertThatThrownBy(() -> service.create(SYLLABUS_ID, TOPIC_ID, linkRequest(EXTERNAL_URL), UPLOADER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Syllabus topic not found");

		verifyNothingStored();
	}

	/**
	 * Pins current behaviour: a link may point at another material's internal download endpoint.
	 * The full-syllabus update rejects that as INVALID_MATERIAL_REFERENCE; this endpoint does not.
	 */
	@Test
	void createAcceptsAnInternalDownloadPathAsALink() {
		givenTopicOf(givenSyllabus(SyllabusStatus.Drafting));
		String otherMaterialDownloadPath = MaterialFileService.downloadPath(99L);

		MaterialFileResponse response = service.create(
				SYLLABUS_ID, TOPIC_ID, linkRequest(otherMaterialDownloadPath), UPLOADER_ID);

		assertThat(response.fileUrl()).isEqualTo("/api/v1/materials/99/download");
		assertThat(response.storedContent()).isFalse();
	}

	@ParameterizedTest(name = "a material of a {0} syllabus can be updated")
	@EnumSource(value = SyllabusStatus.class, names = {"Drafting", "Pending"})
	void updateReplacesTheLinkDetailsAndKeepsTheUploader(SyllabusStatus status) {
		MaterialFile material = givenMaterial(status);

		MaterialFileResponse response = service.update(MATERIAL_ID, new UpdateMaterialFileRequest(
				" week-2.pdf ", " https://cdn.example.com/week-2.pdf ", 4096L, "   "));

		assertThat(material.getFileName()).isEqualTo("week-2.pdf");
		assertThat(material.getFileUrl()).isEqualTo("https://cdn.example.com/week-2.pdf");
		assertThat(material.getFileSize()).isEqualTo(4096L);
		assertThat(material.getContentType()).isNull();
		assertThat(material.getUploadedBy()).isEqualTo(UPLOADER_ID);
		assertThat(material.getUploadedAt()).isEqualTo(ORIGINAL_UPLOADED_AT);
		assertThat(response.fileName()).isEqualTo("week-2.pdf");
		assertThat(response.fileUrl()).isEqualTo("https://cdn.example.com/week-2.pdf");
		verify(auditLogService).record("UPDATE_MATERIAL_FILE", "syllabus", SYLLABUS_ID);
	}

	@ParameterizedTest(name = "a material of an {0} syllabus cannot be updated")
	@EnumSource(value = SyllabusStatus.class, names = {"Active", "Inactive"})
	void updateRejectsMaterialOfPublishedSyllabus(SyllabusStatus status) {
		MaterialFile material = givenMaterial(status);

		assertThatThrownBy(() -> service.update(MATERIAL_ID, updateRequest("https://cdn.example.com/other.pdf")))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("SYLLABUS_NOT_EDITABLE");

		assertThat(material.getFileName()).isEqualTo("week-1.pdf");
		assertThat(material.getFileUrl()).isEqualTo(EXTERNAL_URL);
		verifyNoInteractions(auditLogService);
	}

	@Test
	void updateRejectsUnknownMaterial() {
		when(materialFileRepository.findWithTopicById(MATERIAL_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(MATERIAL_ID, updateRequest(EXTERNAL_URL)))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Material file not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		verifyNoInteractions(auditLogService);
	}

	/**
	 * Pins current behaviour: updating an uploaded material may repoint its URL away from its own
	 * download endpoint (and change its size) while the stored bytes stay behind. The full-syllabus
	 * update guards internal URLs; this endpoint does not.
	 */
	@Test
	void updateCanRepointAnUploadedMaterialAwayFromItsStoredBytes() {
		MaterialFile material = givenMaterial(SyllabusStatus.Drafting);
		material.setFileUrl(MaterialFileService.downloadPath(MATERIAL_ID));
		material.setContentStored(true);

		MaterialFileResponse response = service.update(MATERIAL_ID, updateRequest(EXTERNAL_URL));

		assertThat(response.fileUrl()).isEqualTo(EXTERNAL_URL);
		assertThat(response.storedContent()).isTrue();
		verifyNoInteractions(materialFileContentRepository);
	}

	@ParameterizedTest(name = "a material of a {0} syllabus can be deleted")
	@EnumSource(value = SyllabusStatus.class, names = {"Drafting", "Pending"})
	void deleteRemovesTheMaterialAndAuditsItsSyllabus(SyllabusStatus status) {
		MaterialFile material = givenMaterial(status);

		service.delete(MATERIAL_ID);

		verify(materialFileRepository).delete(material);
		verify(auditLogService).record("DELETE_MATERIAL_FILE", "syllabus", SYLLABUS_ID);
	}

	@ParameterizedTest(name = "a material of an {0} syllabus cannot be deleted")
	@EnumSource(value = SyllabusStatus.class, names = {"Active", "Inactive"})
	void deleteRejectsMaterialOfPublishedSyllabus(SyllabusStatus status) {
		givenMaterial(status);

		assertThatThrownBy(() -> service.delete(MATERIAL_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("SYLLABUS_NOT_EDITABLE");

		verify(materialFileRepository, never()).delete(any());
		verifyNoInteractions(auditLogService);
	}

	@Test
	void deleteRejectsUnknownMaterial() {
		when(materialFileRepository.findWithTopicById(MATERIAL_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(MATERIAL_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Material file not found");

		verify(materialFileRepository, never()).delete(any());
		verifyNoInteractions(auditLogService);
	}

	@Test
	void scopedDeleteRemovesTheMaterialOfTheTopic() {
		givenTopicOf(givenSyllabus(SyllabusStatus.Pending));
		MaterialFile material = new MaterialFile();
		material.setId(MATERIAL_ID);
		when(materialFileRepository.findByIdAndTopicId(MATERIAL_ID, TOPIC_ID)).thenReturn(Optional.of(material));

		service.delete(SYLLABUS_ID, TOPIC_ID, MATERIAL_ID);

		verify(materialFileRepository).delete(material);
		verify(auditLogService).record("DELETE_MATERIAL_FILE", "syllabus", SYLLABUS_ID);
	}

	@ParameterizedTest(name = "a material of an {0} syllabus cannot be deleted through its topic")
	@EnumSource(value = SyllabusStatus.class, names = {"Active", "Inactive"})
	void scopedDeleteRejectsPublishedSyllabus(SyllabusStatus status) {
		givenTopicOf(givenSyllabus(status));

		assertThatThrownBy(() -> service.delete(SYLLABUS_ID, TOPIC_ID, MATERIAL_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("SYLLABUS_NOT_EDITABLE");

		verifyNoInteractions(topicRepository, materialFileRepository, auditLogService);
	}

	@Test
	void scopedDeleteRejectsTopicOfAnotherSyllabus() {
		givenSyllabus(SyllabusStatus.Drafting);
		givenTopicOfAnotherSyllabus();

		assertThatThrownBy(() -> service.delete(SYLLABUS_ID, TOPIC_ID, MATERIAL_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Syllabus topic not found");

		verifyNoInteractions(materialFileRepository, auditLogService);
	}

	@Test
	void scopedDeleteRejectsMaterialOfAnotherTopic() {
		givenTopicOf(givenSyllabus(SyllabusStatus.Drafting));
		MaterialFile material = new MaterialFile();
		material.setId(MATERIAL_ID);
		when(materialFileRepository.findByIdAndTopicId(MATERIAL_ID, OTHER_TOPIC_ID)).thenReturn(Optional.of(material));

		assertThatThrownBy(() -> service.delete(SYLLABUS_ID, TOPIC_ID, MATERIAL_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Material file not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		verify(materialFileRepository, never()).delete(any());
		verifyNoInteractions(auditLogService);
	}

	@Test
	void listReturnsTheTopicMaterialsInRepositoryOrderEvenWhenPublished() {
		// Reading is not gated by editability, so a published syllabus still lists its materials.
		SyllabusTopic topic = givenTopicOf(givenSyllabus(SyllabusStatus.Active));
		MaterialFile newer = newMaterial(topic, 43L, "week-2.pdf");
		MaterialFile older = newMaterial(topic, 42L, "week-1.pdf");
		when(materialFileRepository.findByTopicIdOrderByUploadedAtDesc(TOPIC_ID)).thenReturn(List.of(newer, older));

		List<MaterialFileResponse> materials = service.list(SYLLABUS_ID, TOPIC_ID);

		assertThat(materials).extracting(MaterialFileResponse::id).containsExactly(43L, 42L);
		assertThat(materials).extracting(MaterialFileResponse::topicId).containsOnly(TOPIC_ID);
	}

	@Test
	void listRejectsTopicOfAnotherSyllabus() {
		givenTopicOfAnotherSyllabus();

		assertThatThrownBy(() -> service.list(SYLLABUS_ID, TOPIC_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Syllabus topic not found");

		verifyNoInteractions(materialFileRepository);
	}

	@Test
	void listLibraryDefaultsToNewestFirstAndTrimsTheKeyword() {
		SyllabusTopic topic = topicOf(newSyllabus(SYLLABUS_ID, SyllabusStatus.Active));
		when(materialFileRepository.search(any(), any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(newMaterial(topic, 42L, "week-1.pdf"))));

		Page<MaterialFileResponse> page = service.listLibrary(SYLLABUS_ID, TOPIC_ID, "  week ", 2, 20);

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(materialFileRepository).search(eq(SYLLABUS_ID), eq(TOPIC_ID), eq("week"), pageable.capture());
		assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
		assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
		assertThat(pageable.getValue().getSort()).isEqualTo(NEWEST_FIRST);
		assertThat(page.getContent()).extracting(MaterialFileResponse::id).containsExactly(42L);
	}

	@ParameterizedTest(name = "keyword [{0}] means no keyword filter")
	@CsvSource({"'   '", "''", "'\t'"})
	void listLibraryTreatsBlankKeywordAsNoFilter(String keyword) {
		when(materialFileRepository.search(any(), any(), any(), any())).thenReturn(Page.empty());

		service.listLibrary(null, null, keyword, 0, 10);

		verify(materialFileRepository).search(isNull(), isNull(), isNull(), any(Pageable.class));
	}

	@Test
	void listLibrarySortsByAnAllowedField() {
		when(materialFileRepository.search(any(), any(), any(), any())).thenReturn(Page.empty());

		service.listLibrary(null, null, null, 0, 10, "fileName", "asc");

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(materialFileRepository).search(isNull(), isNull(), isNull(), pageable.capture());
		assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "fileName"));
	}

	@ParameterizedTest(name = "sort [{0} {1}] is rejected as {2}")
	@CsvSource({
			"fileUrl, asc, INVALID_SORT_FIELD",
			"uploadedBy, desc, INVALID_SORT_FIELD",
			"fileName, sideways, INVALID_SORT_ORDER"
	})
	void listLibraryRejectsUnsupportedSort(String sortBy, String order, String expectedCode) {
		assertThatThrownBy(() -> service.listLibrary(null, null, null, 0, 10, sortBy, order))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo(expectedCode);

		verifyNoInteractions(materialFileRepository);
	}

	@Test
	void assignedToUserSearchesOnlyEnrolledAndCompletedClassesNewestFirst() {
		Syllabus syllabus = newSyllabus(SYLLABUS_ID, SyllabusStatus.Active);
		MaterialFile material = newMaterial(topicOf(syllabus), 42L, "week-1.pdf");
		when(materialFileRepository.searchAssignedToUser(anyLong(), any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(material)));

		Page<AssignedMaterialFileResponse> page = service.assignedToUser(TRAINEE_ID, " java ", 0, 10);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Collection<ClassEnrollmentStatus>> statuses = ArgumentCaptor.forClass(Collection.class);
		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(materialFileRepository).searchAssignedToUser(
				eq(TRAINEE_ID), statuses.capture(), eq("java"), pageable.capture());
		assertThat(statuses.getValue()).containsExactlyInAnyOrder(
				ClassEnrollmentStatus.Enrolled,
				ClassEnrollmentStatus.Completed);
		assertThat(pageable.getValue().getSort()).isEqualTo(NEWEST_FIRST);
		assertThat(page.getContent()).singleElement().satisfies(assigned -> {
			assertThat(assigned.id()).isEqualTo(42L);
			assertThat(assigned.syllabusId()).isEqualTo(SYLLABUS_ID);
			assertThat(assigned.syllabusCode()).isEqualTo(syllabus.getCode());
			assertThat(assigned.topicId()).isEqualTo(TOPIC_ID);
		});
	}

	@Test
	void assignedToUserRejectsUnsupportedSortField() {
		assertThatThrownBy(() -> service.assignedToUser(TRAINEE_ID, null, 0, 10, "fileUrl", "asc"))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_SORT_FIELD");

		verifyNoInteractions(materialFileRepository);
	}

	private Syllabus givenSyllabus(SyllabusStatus status) {
		Syllabus syllabus = newSyllabus(SYLLABUS_ID, status);
		when(syllabusRepository.findById(SYLLABUS_ID)).thenReturn(Optional.of(syllabus));
		return syllabus;
	}

	private SyllabusTopic givenTopicOf(Syllabus syllabus) {
		SyllabusTopic topic = topicOf(syllabus);
		when(topicRepository.findByIdAndUnitDaySyllabusId(TOPIC_ID, syllabus.getId())).thenReturn(Optional.of(topic));
		return topic;
	}

	/** The topic id exists, but only when looked up through another syllabus. */
	private void givenTopicOfAnotherSyllabus() {
		givenTopicOf(newSyllabus(OTHER_SYLLABUS_ID, SyllabusStatus.Drafting));
	}

	/** An external link material, loaded with its topic graph as the id-only endpoints load it. */
	private MaterialFile givenMaterial(SyllabusStatus status) {
		MaterialFile material = newMaterial(topicOf(newSyllabus(SYLLABUS_ID, status)), MATERIAL_ID, "week-1.pdf");
		when(materialFileRepository.findWithTopicById(MATERIAL_ID)).thenReturn(Optional.of(material));
		return material;
	}

	private MaterialFile captureSaved() {
		ArgumentCaptor<MaterialFile> saved = ArgumentCaptor.forClass(MaterialFile.class);
		verify(materialFileRepository).save(saved.capture());
		return saved.getValue();
	}

	private void verifyNothingStored() {
		verify(materialFileRepository, never()).save(any());
		verifyNoInteractions(materialFileContentRepository, auditLogService);
	}

	private void verifyUploadCountedOnceAsFailure() {
		verify(domainMetrics).recordUpload(false);
		verify(domainMetrics, never()).recordUpload(true);
	}

	private static MultipartFile pdf(String originalFileName) {
		return new MockMultipartFile("file", originalFileName, "application/pdf", CONTENT);
	}

	private static CreateMaterialFileRequest linkRequest(String fileUrl) {
		return new CreateMaterialFileRequest("week-1.pdf", fileUrl, 2048L, "application/pdf");
	}

	private static UpdateMaterialFileRequest updateRequest(String fileUrl) {
		return new UpdateMaterialFileRequest("week-2.pdf", fileUrl, 4096L, "application/pdf");
	}

	private static Syllabus newSyllabus(long id, SyllabusStatus status) {
		Syllabus syllabus = new Syllabus();
		syllabus.setId(id);
		syllabus.setCode("JAVA-" + id);
		syllabus.setName("Java Fundamentals");
		syllabus.setStatus(status);
		return syllabus;
	}

	private static SyllabusTopic topicOf(Syllabus syllabus) {
		SyllabusDay day = new SyllabusDay();
		day.setId(20L);
		day.setSyllabus(syllabus);
		SyllabusUnit unit = new SyllabusUnit();
		unit.setId(30L);
		unit.setDay(day);
		SyllabusTopic topic = new SyllabusTopic();
		topic.setId(TOPIC_ID);
		topic.setName("Generics");
		topic.setUnit(unit);
		return topic;
	}

	private static MaterialFile newMaterial(SyllabusTopic topic, long id, String fileName) {
		MaterialFile material = new MaterialFile();
		material.setId(id);
		material.setTopic(topic);
		material.setFileName(fileName);
		material.setFileUrl(EXTERNAL_URL);
		material.setFileSize(1024L);
		material.setContentType("application/pdf");
		material.setUploadedBy(UPLOADER_ID);
		material.setUploadedAt(ORIGINAL_UPLOADED_AT);
		return material;
	}
}
