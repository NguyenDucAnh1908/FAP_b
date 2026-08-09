package com.fap.syllabus.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ForbiddenException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.metrics.DomainMetrics;
import com.fap.common.util.FileValidator;
import com.fap.syllabus.dto.MaterialFileDownload;
import com.fap.syllabus.entity.MaterialFile;
import com.fap.syllabus.entity.MaterialFileContent;
import com.fap.syllabus.mapper.MaterialFileMapper;
import com.fap.syllabus.repository.MaterialFileContentRepository;
import com.fap.syllabus.repository.MaterialFileRepository;
import com.fap.syllabus.repository.SyllabusRepository;
import com.fap.syllabus.repository.SyllabusTopicRepository;
import com.fap.training.enums.TrainingRegistrationStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the download ownership rule. Trainees hold {@code learning_material:view} globally, so the
 * controller's method-level check cannot be the only gate — these tests pin the service-side probe
 * that decides whether a specific user may read a specific material.
 */
class MaterialDownloadAuthorizationTest {

	private static final long MATERIAL_ID = 41L;
	private static final long TRAINEE_ID = 7L;
	private static final byte[] FILE_BYTES = "syllabus slides".getBytes(StandardCharsets.UTF_8);

	private static final boolean CAN_MANAGE = true;
	private static final boolean CANNOT_MANAGE = false;

	private final SyllabusRepository syllabusRepository = mock(SyllabusRepository.class);
	private final SyllabusTopicRepository topicRepository = mock(SyllabusTopicRepository.class);
	private final MaterialFileRepository materialFileRepository = mock(MaterialFileRepository.class);
	private final MaterialFileContentRepository materialFileContentRepository =
			mock(MaterialFileContentRepository.class);
	private final MaterialFileMapper materialFileMapper = mock(MaterialFileMapper.class);
	private final FileValidator fileValidator = mock(FileValidator.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final DomainMetrics domainMetrics = mock(DomainMetrics.class);

	private final MaterialFileService service = new MaterialFileService(
			syllabusRepository,
			topicRepository,
			materialFileRepository,
			materialFileContentRepository,
			materialFileMapper,
			fileValidator,
			auditLogService,
			domainMetrics);

	private MaterialFile givenMaterial(String contentType) {
		MaterialFile materialFile = new MaterialFile();
		materialFile.setId(MATERIAL_ID);
		materialFile.setFileName("week-1.pdf");
		materialFile.setContentType(contentType);
		when(materialFileRepository.findWithTopicById(MATERIAL_ID)).thenReturn(Optional.of(materialFile));
		return materialFile;
	}

	private void givenStoredContent() {
		MaterialFileContent content = new MaterialFileContent();
		content.setFileData(FILE_BYTES);
		when(materialFileContentRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(content));
	}

	private void givenRegistrationProbeReturns(boolean assigned) {
		when(materialFileRepository.existsAssignedToUser(eq(MATERIAL_ID), eq(TRAINEE_ID), any()))
				.thenReturn(assigned);
	}

	@Test
	void traineeAssignedToTheMaterialReceivesTheBytes() {
		givenMaterial("application/pdf");
		givenStoredContent();
		givenRegistrationProbeReturns(true);

		MaterialFileDownload download = service.download(MATERIAL_ID, TRAINEE_ID, CANNOT_MANAGE);

		assertThat(download.fileName()).isEqualTo("week-1.pdf");
		assertThat(download.contentType()).isEqualTo("application/pdf");
		assertThat(download.data()).isEqualTo(FILE_BYTES);
	}

	@Test
	void traineeNotRegisteredForTheMaterialIsForbidden() {
		givenMaterial("application/pdf");
		givenStoredContent();
		givenRegistrationProbeReturns(false);

		assertThatThrownBy(() -> service.download(MATERIAL_ID, TRAINEE_ID, CANNOT_MANAGE))
				.isInstanceOf(ForbiddenException.class);

		// Bytes must not even be fetched for a user who fails the ownership check.
		verify(materialFileContentRepository, never()).findById(anyLong());
	}

	@Test
	void staffWithManagePermissionSkipsTheRegistrationProbe() {
		givenMaterial("application/pdf");
		givenStoredContent();

		MaterialFileDownload download = service.download(MATERIAL_ID, TRAINEE_ID, CAN_MANAGE);

		assertThat(download.data()).isEqualTo(FILE_BYTES);
		verify(materialFileRepository, never())
				.existsAssignedToUser(anyLong(), anyLong(), any());
	}

	@Test
	void materialWithoutStoredBytesIsNotFound() {
		givenMaterial("application/pdf");
		when(materialFileContentRepository.findById(MATERIAL_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.download(MATERIAL_ID, TRAINEE_ID, CAN_MANAGE))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void unknownMaterialIsNotFound() {
		when(materialFileRepository.findWithTopicById(MATERIAL_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.download(MATERIAL_ID, TRAINEE_ID, CAN_MANAGE))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void materialWithoutContentTypeFallsBackToOctetStream() {
		givenMaterial(null);
		givenStoredContent();

		MaterialFileDownload download = service.download(MATERIAL_ID, TRAINEE_ID, CAN_MANAGE);

		assertThat(download.contentType()).isEqualTo("application/octet-stream");
	}

	@Test
	void ownershipProbeOnlyAcceptsRegisteredAndCompletedRegistrations() {
		givenMaterial("application/pdf");
		givenStoredContent();
		givenRegistrationProbeReturns(true);

		service.download(MATERIAL_ID, TRAINEE_ID, CANNOT_MANAGE);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Collection<TrainingRegistrationStatus>> statusesCaptor =
				ArgumentCaptor.forClass(Collection.class);
		verify(materialFileRepository).existsAssignedToUser(
				eq(MATERIAL_ID), eq(TRAINEE_ID), statusesCaptor.capture());
		assertThat(statusesCaptor.getValue()).containsExactlyInAnyOrder(
				TrainingRegistrationStatus.Registered,
				TrainingRegistrationStatus.Completed);
	}

	@Test
	void downloadPathPointsAtTheDownloadEndpoint() {
		assertThat(MaterialFileService.downloadPath(MATERIAL_ID))
				.isEqualTo("/api/v1/materials/41/download");
	}
}
