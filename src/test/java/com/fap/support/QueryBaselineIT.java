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
 * Measures database round trips of the read paths the optimization plan targets (N+1 hot spots)
 * against the seeded data, and writes them to {@code target/query-baseline.txt}.
 *
 * <p>These are measurements, not budgets: once a path is optimized its budget moves into that
 * module's own {@code *IT} as an assertion. Running the paths on Oracle also proves their queries
 * execute there.
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

		record("myLearning.learningContent", measure(() -> myLearningService.learningContent(classId, userId, null)));
		record("myLearning.progress", measure(() -> myLearningService.progress(classId, userId)));
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

		record("courseResult.list(results=" + row[1] + ")", measure(() -> courseResultService.list(classId)));
	}

	@Test
	void adminDashboard() {
		record("adminDashboard.getDashboard", measure(() -> adminDashboardService.getDashboard()));
	}

	private Object[] first(String jpql) {
		List<Object[]> rows = entityManager.createQuery(jpql, Object[].class).setMaxResults(1).getResultList();
		assumeThat(rows).as("seed data for: " + jpql).isNotEmpty();
		return rows.get(0);
	}

	private static void record(String path, Measured<?> measured) {
		assertThat(measured.result()).isNotNull();
		RESULTS.put(path, measured.statements() + " | " + measured.entitiesLoaded());
	}
}
