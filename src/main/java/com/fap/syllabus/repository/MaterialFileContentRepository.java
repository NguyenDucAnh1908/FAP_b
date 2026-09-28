package com.fap.syllabus.repository;

import com.fap.syllabus.entity.MaterialFileContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface MaterialFileContentRepository
		extends JpaRepository<MaterialFileContent, Long>, MaterialFileContentStreams {

	@Query("""
			select c.materialFileId
			from MaterialFileContent c
			where c.materialFile.topic.unit.day.syllabus.id = :syllabusId
			""")
	Set<Long> findStoredMaterialIdsBySyllabusId(@Param("syllabusId") Long syllabusId);

	/** Copies stored bytes to another material inside Oracle, without moving them through the JVM. */
	@Modifying
	@Query(
			value = """
					INSERT INTO material_file_contents (material_file_id, file_data)
					SELECT :targetId, file_data FROM material_file_contents WHERE material_file_id = :sourceId
					""",
			nativeQuery = true)
	int copyStoredContent(@Param("sourceId") Long sourceMaterialFileId, @Param("targetId") Long targetMaterialFileId);
}
