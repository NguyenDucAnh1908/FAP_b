package com.fap.syllabus.repository;

import com.fap.syllabus.entity.MaterialFile;
import com.fap.training.enums.TrainingRegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MaterialFileRepository extends JpaRepository<MaterialFile, Long> {

	@EntityGraph(attributePaths = {"topic", "topic.unit", "topic.unit.day", "topic.unit.day.syllabus"})
	List<MaterialFile> findByTopicIdOrderByUploadedAtDesc(Long topicId);

	@EntityGraph(attributePaths = {"topic", "topic.unit", "topic.unit.day", "topic.unit.day.syllabus"})
	Optional<MaterialFile> findByIdAndTopicId(Long id, Long topicId);

	@EntityGraph(attributePaths = {"topic", "topic.unit", "topic.unit.day", "topic.unit.day.syllabus"})
	Optional<MaterialFile> findWithTopicById(Long id);

	@EntityGraph(attributePaths = {"topic", "topic.unit", "topic.unit.day", "topic.unit.day.syllabus"})
	@Query(
			value = """
					select m
					from MaterialFile m
					where (:syllabusId is null or m.topic.unit.day.syllabus.id = :syllabusId)
					  and (:topicId is null or m.topic.id = :topicId)
					  and (:keyword is null
					       or lower(m.fileName) like concat(concat('%', lower(:keyword)), '%')
					       or lower(m.fileUrl) like concat(concat('%', lower(:keyword)), '%')
					       or lower(coalesce(m.contentType, '')) like concat(concat('%', lower(:keyword)), '%'))
					""",
			countQuery = """
					select count(m)
					from MaterialFile m
					where (:syllabusId is null or m.topic.unit.day.syllabus.id = :syllabusId)
					  and (:topicId is null or m.topic.id = :topicId)
					  and (:keyword is null
					       or lower(m.fileName) like concat(concat('%', lower(:keyword)), '%')
					       or lower(m.fileUrl) like concat(concat('%', lower(:keyword)), '%')
					       or lower(coalesce(m.contentType, '')) like concat(concat('%', lower(:keyword)), '%'))
					""")
	Page<MaterialFile> search(
			@Param("syllabusId") Long syllabusId,
			@Param("topicId") Long topicId,
			@Param("keyword") String keyword,
			Pageable pageable);

	@EntityGraph(attributePaths = {"topic", "topic.unit", "topic.unit.day", "topic.unit.day.syllabus"})
	@Query(
			value = """
					select distinct m
					from MaterialFile m
					join TrainingProgramSyllabus tps on tps.syllabus = m.topic.unit.day.syllabus
					join FapClass c on c.trainingProgram = tps.program
					join TrainingSession s on s.fapClass = c
					join TrainingRegistration r on r.trainingSession = s
					where r.user.id = :userId
					  and r.status in :eligibleStatuses
					  and (:keyword is null
					       or lower(m.fileName) like concat(concat('%', lower(:keyword)), '%')
					       or lower(m.fileUrl) like concat(concat('%', lower(:keyword)), '%')
					       or lower(coalesce(m.contentType, '')) like concat(concat('%', lower(:keyword)), '%')
					       or lower(m.topic.name) like concat(concat('%', lower(:keyword)), '%')
					       or lower(m.topic.unit.day.syllabus.name) like concat(concat('%', lower(:keyword)), '%')
					       or lower(m.topic.unit.day.syllabus.code) like concat(concat('%', lower(:keyword)), '%'))
					""",
			countQuery = """
					select count(distinct m)
					from MaterialFile m
					join TrainingProgramSyllabus tps on tps.syllabus = m.topic.unit.day.syllabus
					join FapClass c on c.trainingProgram = tps.program
					join TrainingSession s on s.fapClass = c
					join TrainingRegistration r on r.trainingSession = s
					where r.user.id = :userId
					  and r.status in :eligibleStatuses
					  and (:keyword is null
					       or lower(m.fileName) like concat(concat('%', lower(:keyword)), '%')
					       or lower(m.fileUrl) like concat(concat('%', lower(:keyword)), '%')
					       or lower(coalesce(m.contentType, '')) like concat(concat('%', lower(:keyword)), '%')
					       or lower(m.topic.name) like concat(concat('%', lower(:keyword)), '%')
					       or lower(m.topic.unit.day.syllabus.name) like concat(concat('%', lower(:keyword)), '%')
					       or lower(m.topic.unit.day.syllabus.code) like concat(concat('%', lower(:keyword)), '%'))
					""")
	Page<MaterialFile> searchAssignedToUser(
			@Param("userId") Long userId,
			@Param("eligibleStatuses") Collection<TrainingRegistrationStatus> eligibleStatuses,
			@Param("keyword") String keyword,
			Pageable pageable);

	@EntityGraph(attributePaths = {"topic", "topic.unit", "topic.unit.day", "topic.unit.day.syllabus"})
	@Query("""
			select distinct m
			from MaterialFile m
			join TrainingProgramSyllabus tps on tps.syllabus = m.topic.unit.day.syllabus
			join FapClass c on c.trainingProgram = tps.program
			join TrainingSession s on s.fapClass = c
			join TrainingRegistration r on r.trainingSession = s
			where r.user.id = :userId
			  and c.id = :classId
			  and r.status in :eligibleStatuses
			  and (:keyword is null
			       or lower(m.fileName) like concat(concat('%', lower(:keyword)), '%')
			       or lower(m.fileUrl) like concat(concat('%', lower(:keyword)), '%')
			       or lower(coalesce(m.contentType, '')) like concat(concat('%', lower(:keyword)), '%')
			       or lower(m.topic.name) like concat(concat('%', lower(:keyword)), '%')
			       or lower(m.topic.unit.day.syllabus.name) like concat(concat('%', lower(:keyword)), '%')
			       or lower(m.topic.unit.day.syllabus.code) like concat(concat('%', lower(:keyword)), '%'))
			""")
	List<MaterialFile> findAssignedToUserByClass(
			@Param("userId") Long userId,
			@Param("classId") Long classId,
			@Param("eligibleStatuses") Collection<TrainingRegistrationStatus> eligibleStatuses,
			@Param("keyword") String keyword);

	/**
	 * Ownership probe for downloads. Trainees hold {@code learning_material:view} globally, so the
	 * method-level permission check alone would expose every material to every trainee; this walks
	 * the same eligibility chain as {@link #searchAssignedToUser} to confirm the user is actually
	 * registered for a session that teaches the material.
	 */
	@Query("""
			select count(m) > 0
			from MaterialFile m
			join TrainingProgramSyllabus tps on tps.syllabus = m.topic.unit.day.syllabus
			join FapClass c on c.trainingProgram = tps.program
			join TrainingSession s on s.fapClass = c
			join TrainingRegistration r on r.trainingSession = s
			where m.id = :materialId
			  and r.user.id = :userId
			  and r.status in :eligibleStatuses
			""")
	boolean existsAssignedToUser(
			@Param("materialId") Long materialId,
			@Param("userId") Long userId,
			@Param("eligibleStatuses") Collection<TrainingRegistrationStatus> eligibleStatuses);
}
