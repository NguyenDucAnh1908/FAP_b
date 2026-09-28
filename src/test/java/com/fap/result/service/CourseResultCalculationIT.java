package com.fap.result.service;

import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassStatus;
import com.fap.common.security.RoleNames;
import com.fap.result.dto.ClassCourseResultsResponse;
import com.fap.result.enums.CourseResultStatus;
import com.fap.support.AbstractOracleIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Runs the class-wide result calculation on Oracle: the bulk snapshot delete, the best-attempt
 * projection and the class-scoped registration and attendance reads.
 */
class CourseResultCalculationIT extends AbstractOracleIT {

	@Autowired
	private CourseResultService courseResultService;

	private Long classId;
	private Long actorId;

	@BeforeEach
	void pickClassWithMostEnrollments() {
		List<Long> classIds = entityManager.createQuery("""
						select e.fapClass.id
						from ClassEnrollment e
						group by e.fapClass.id
						order by count(e) desc, e.fapClass.id
						""", Long.class)
				.getResultList();
		assumeThat(classIds).as("seed data has classes with enrollments").isNotEmpty();
		classId = classIds.get(0);
		// Calculation is only allowed for active classes; the change is rolled back with the test.
		entityManager.find(FapClass.class, classId).setStatus(ClassStatus.Active);
		actorId = entityManager.createQuery(
						"select u.id from User u join u.roles r where r.name = :roleName order by u.id", Long.class)
				.setParameter("roleName", RoleNames.SUPER_ADMIN)
				.setMaxResults(1)
				.getSingleResult();
		flushAndClear();
	}

	@Test
	void recalculationIsStable() {
		ClassCourseResultsResponse first = courseResultService.calculate(classId, actorId);
		flushAndClear();
		ClassCourseResultsResponse second = courseResultService.calculate(classId, actorId);

		assertThat(first.results()).isNotEmpty();
		assertThat(outcomes(second)).isEqualTo(outcomes(first));
		assertThat(second.results()).allSatisfy(result -> {
			if (result.status() != CourseResultStatus.Withdrawn) {
				assertThat(result.quizzes()).hasSize(result.requiredQuizCount());
			}
		});
	}

	@Test
	void listingIsConstantInQueries() {
		courseResultService.calculate(classId, actorId);

		Measured<ClassCourseResultsResponse> measured = measure(() -> courseResultService.list(classId));

		assertThat(measured.result().results()).isNotEmpty();
		// Class, results, quiz snapshots, adjustments: independent of the number of results.
		assertThat(measured.statements()).isLessThanOrEqualTo(5);
	}

	@Test
	void calculationDoesNotQueryPerEnrollment() {
		courseResultService.calculate(classId, actorId);

		Measured<ClassCourseResultsResponse> measured = measure(() -> courseResultService.calculate(classId, actorId));

		// 13 with the seed class (lock, class-wide reads, bulk delete, batched writes, audit, list). Every
		// read is class-wide and writes are JDBC-batched, so the count does not grow with enrollments.
		assertThat(measured.statements()).as("statements for %d results", measured.result().results().size())
				.isLessThanOrEqualTo(15);
	}

	private static List<String> outcomes(ClassCourseResultsResponse response) {
		return response.results().stream()
				.map(result -> result.userId() + ":" + result.status() + ":" + result.attendedSessions()
						+ ":" + result.passedQuizCount())
				.toList();
	}
}
