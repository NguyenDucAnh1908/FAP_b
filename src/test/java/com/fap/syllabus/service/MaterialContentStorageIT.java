package com.fap.syllabus.service;

import com.fap.common.security.RoleNames;
import com.fap.support.AbstractOracleIT;
import com.fap.syllabus.dto.CloneSyllabusRequest;
import com.fap.syllabus.dto.FullSyllabusResponse;
import com.fap.syllabus.dto.MaterialFileDownload;
import com.fap.syllabus.dto.MaterialFileResponse;
import com.fap.syllabus.entity.MaterialFile;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.entity.SyllabusTopic;
import com.fap.syllabus.enums.SyllabusStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Upload streams into the BLOB, download streams it back out through a spooled file, and a version
 * clone copies it inside Oracle. All three must round-trip the exact bytes.
 */
class MaterialContentStorageIT extends AbstractOracleIT {

	private static final byte[] CONTENT = randomBytes(3 * 1024 * 1024 + 17);

	@Autowired
	private MaterialFileService materialFileService;

	@Autowired
	private SyllabusVersionService syllabusVersionService;

	private SyllabusTopic topic;
	private Long userId;

	@BeforeEach
	void pickEditableTopic() {
		List<SyllabusTopic> topics = entityManager.createQuery("""
						select t from SyllabusTopic t
						where t.unit.day.syllabus.status in (com.fap.syllabus.enums.SyllabusStatus.Drafting,
						                                    com.fap.syllabus.enums.SyllabusStatus.Pending)
						order by t.id
						""", SyllabusTopic.class)
				.setMaxResults(1)
				.getResultList();
		assumeThat(topics).as("seed data has an editable syllabus topic").isNotEmpty();
		topic = topics.get(0);
		userId = entityManager.createQuery(
						"select u.id from User u join u.roles r where r.name = :roleName order by u.id", Long.class)
				.setParameter("roleName", RoleNames.SUPER_ADMIN)
				.setMaxResults(1)
				.getSingleResult();
	}

	@Test
	void uploadedBytesDownloadUnchanged() throws IOException {
		MaterialFileResponse uploaded = upload();

		assertThat(uploaded.fileSize()).isEqualTo(CONTENT.length);
		assertThat(downloadBytes(uploaded.id())).isEqualTo(CONTENT);
	}

	@Test
	void cloneCopiesStoredBytesInsideTheDatabase() throws IOException {
		MaterialFileResponse uploaded = upload();
		Syllabus source = entityManager.find(Syllabus.class, topic.getUnit().getDay().getSyllabus().getId());
		source.setStatus(SyllabusStatus.Active);
		flushAndClear();

		FullSyllabusResponse clone = syllabusVersionService.cloneVersion(
				source.getId(),
				new CloneSyllabusRequest("IT clone", "IT_CLONE_" + uploaded.id(), "v-it"),
				userId);
		flushAndClear();

		Long clonedMaterialId = entityManager.createQuery("""
						select m.id from MaterialFile m
						where m.topic.unit.day.syllabus.id = :syllabusId and m.fileName = :fileName
						""", Long.class)
				.setParameter("syllabusId", clone.syllabus().id())
				.setParameter("fileName", uploaded.fileName())
				.getSingleResult();
		assertThat(entityManager.find(MaterialFile.class, clonedMaterialId).isContentStored()).isTrue();
		assertThat(downloadBytes(clonedMaterialId)).isEqualTo(CONTENT);
	}

	private MaterialFileResponse upload() {
		MaterialFileResponse response = materialFileService.upload(
				topic.getUnit().getDay().getSyllabus().getId(),
				topic.getId(),
				new MockMultipartFile("file", "it-material.pdf", "application/pdf", CONTENT),
				userId);
		assertThat(response.storedContent()).isTrue();
		flushAndClear();
		return response;
	}

	private byte[] downloadBytes(Long materialId) throws IOException {
		MaterialFileDownload download = materialFileService.download(materialId, userId, true);
		assertThat(download.contentLength()).isEqualTo(CONTENT.length);
		try (InputStream content = download.content()) {
			return content.readAllBytes();
		}
	}

	private static byte[] randomBytes(int length) {
		byte[] bytes = new byte[length];
		new Random(42).nextBytes(bytes);
		return bytes;
	}
}
