package com.fap.syllabus.repository;

import com.fap.syllabus.entity.MaterialFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialFileRepository extends JpaRepository<MaterialFile, Long> {

	List<MaterialFile> findByTopicIdOrderByUploadedAtDesc(Long topicId);

	Optional<MaterialFile> findByIdAndTopicId(Long id, Long topicId);
}
