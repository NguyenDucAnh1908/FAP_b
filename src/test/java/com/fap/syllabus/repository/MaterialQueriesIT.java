package com.fap.syllabus.repository;

import com.fap.clazz.enums.ClassEnrollmentStatus;
import com.fap.syllabus.entity.MaterialFile;
import com.fap.support.AbstractOracleIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Regression for ORA-00932: the trainee material queries used to join enrollments and DISTINCT the
 * rows, which Oracle rejects because the fetched Syllabus has CLOB columns.
 */
class MaterialQueriesIT extends AbstractOracleIT {

	private static final List<ClassEnrollmentStatus> ELIGIBLE = List.of(
			ClassEnrollmentStatus.Enrolled,
			ClassEnrollmentStatus.Completed);

	@Autowired
	private MaterialFileRepository materialFileRepository;

	private Long userId;
	private Long classId;

	@BeforeEach
	void pickEnrollmentWithMaterials() {
		List<Object[]> rows = entityManager.createQuery("""
						select e.user.id, e.fapClass.id
						from ClassEnrollment e
						join TrainingProgramSyllabus tps on tps.program = e.fapClass.trainingProgram
						join MaterialFile m on m.topic.unit.day.syllabus = tps.syllabus
						where e.status in :eligible
						group by e.user.id, e.fapClass.id
						order by count(m) desc, e.user.id
						""", Object[].class)
				.setParameter("eligible", ELIGIBLE)
				.setMaxResults(1)
				.getResultList();
		assumeThat(rows).as("seed data has an enrollment whose program has materials").isNotEmpty();
		userId = (Long) rows.get(0)[0];
		classId = (Long) rows.get(0)[1];
	}

	@Test
	void assignedMaterialsAreListedOnceEach() {
		Page<MaterialFile> page = materialFileRepository.searchAssignedToUser(userId, ELIGIBLE, null, PageRequest.of(0, 100));
		List<MaterialFile> byClass = materialFileRepository.findAssignedToUserByClass(userId, classId, ELIGIBLE, null);

		assertThat(byClass).isNotEmpty().doesNotHaveDuplicates();
		assertThat(page.getContent()).doesNotHaveDuplicates().containsAll(byClass);
		assertThat(page.getTotalElements()).isEqualTo(page.getContent().size());
		assertThat(materialFileRepository.existsAssignedToUser(byClass.get(0).getId(), userId, ELIGIBLE)).isTrue();
	}

	@Test
	void keywordFilterStillMatchesTopicAndSyllabusFields() {
		MaterialFile material = materialFileRepository.findAssignedToUserByClass(userId, classId, ELIGIBLE, null).get(0);
		String syllabusCode = material.getTopic().getUnit().getDay().getSyllabus().getCode();

		List<MaterialFile> matches = materialFileRepository.findAssignedToUserByClass(
				userId, classId, ELIGIBLE, syllabusCode.toLowerCase());

		assertThat(matches).contains(material);
	}
}
