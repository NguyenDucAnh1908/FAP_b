package com.fap.support;

import com.fap.dashboard.service.AdminDashboardService;
import com.fap.result.service.CourseResultService;
import com.fap.training.service.MyLearningService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Measures database round trips of the heavy read paths against the seeded data, writes them to
 * {@code target/query-baseline.txt}, and fails when a path exceeds its budget (an N+1 regression).
 *
 * <p>Budgets are the optimized counts (docs/optimization-plan.md lists the before/after figures).
 * They do not grow with the number of rows, so seed data changes should not break them.
 */
class QueryBaselineIT extends AbstractOracleIT {

	private static final Map<String, String> RESULTS = new TreeMap<>();

	@Autowired
	private MyLearningService myLearningService;

	@Autowired
	private CourseResultService courseResultService;

	@Autowired
	private AdminDashboardService adminDashboardService;

	@AfterAll
	static void writeReport() throws IOException {
		StringBuilder report = new StringBuilder("# path | statements | entities loaded\n");
		RESULTS.forEach((path, value) -> report.append(path).append(" | ").append(value).append('\n'));
		Files.createDirectories(Path.of("target"));
		Files.writeString(Path.of("target", "query-baseline.txt"), report.toString());
	}

	@Test
	void myLearningForBusiestEnrollment() {
		Object[] enrollment = first("""
				select e.user.id, e.fapClass.id
				from ClassEnrollment e
				where e.status in (com.fap.clazz.enums.ClassEnrollmentStatus.Enrolled,
				                   com.fap.clazz.enums.ClassEnrollmentStatus.Completed)
				  and e.fapClass.trainingProgram is not null
				order by (select count(qa) from QuizAssignment qa where qa.fapClass = e.fapClass) desc, e.id
				""");
		Long userId = (Long) enrollment[0];
		Long classId = (Long) enrollment[1];

		// 23 and 15 before per-quiz lookups were grouped.
		record("myLearning.learningContent", measure(() -> myLearningService.learningContent(classId, userId, null)), 12);
		record("myLearning.progress", measure(() -> myLearningService.progress(classId, userId)), 10);
	}

	@Test
	void courseResultsForLargestClass() {
		Object[] row = first("""
				select r.fapClass.id, count(r)
				from CourseResult r
				group by r.fapClass.id
				order by count(r) desc, r.fapClass.id
				""");
		Long classId = (Long) row[0];

		// 12 for 5 results (2 per result + 2) before children were read class-wide.
		record("courseResult.list(results=" + row[1] + ")", measure(() -> courseResultService.list(classId)), 5);
	}

	@Test
	void adminDashboard() {
		record("adminDashboard.getDashboard", measure(() -> adminDashboardService.getDashboard()), 16);
	}

	private Object[] first(String jpql) {
		List<Object[]> rows = entityManager.createQuery(jpql, Object[].class).setMaxResults(1).getResultList();
		assumeThat(rows).as("seed data for: " + jpql).isNotEmpty();
		return rows.get(0);
	}

	private static void record(String path, Measured<?> measured, long statementBudget) {
		assertThat(measured.result()).isNotNull();
		RESULTS.put(path, measured.statements() + " | " + measured.entitiesLoaded());
		assertThat(measured.statements()).as(path + " statements").isLessThanOrEqualTo(statementBudget);
	}
}
