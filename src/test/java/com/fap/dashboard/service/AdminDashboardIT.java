package com.fap.dashboard.service;

import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.repository.ClassRepository;
import com.fap.dashboard.dto.AdminDashboardResponse;
import com.fap.program.enums.TrainingProgramStatus;
import com.fap.program.repository.TrainingProgramRepository;
import com.fap.quiz.enums.QuizAttemptStatus;
import com.fap.quiz.enums.QuizStatus;
import com.fap.quiz.repository.QuizAttemptRepository;
import com.fap.quiz.repository.QuizRepository;
import com.fap.support.AbstractOracleIT;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.repository.SyllabusRepository;
import com.fap.user.enums.UserStatus;
import com.fap.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The dashboard reads grouped counts; each figure must still equal the per-status count it replaced.
 * The it profile disables the dashboard cache, so every call builds a fresh response.
 */
class AdminDashboardIT extends AbstractOracleIT {

	@Autowired
	private AdminDashboardService adminDashboardService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SyllabusRepository syllabusRepository;

	@Autowired
	private TrainingProgramRepository trainingProgramRepository;

	@Autowired
	private ClassRepository classRepository;

	@Autowired
	private QuizRepository quizRepository;

	@Autowired
	private QuizAttemptRepository quizAttemptRepository;

	@Test
	void groupedCountsMatchPerStatusCounts() {
		AdminDashboardResponse dashboard = adminDashboardService.getDashboard();

		AdminDashboardResponse.UserSummary users = dashboard.users();
		assertThat(users.totalUsers()).isEqualTo(userRepository.count());
		assertThat(users.activeUsers()).isEqualTo(userRepository.countByStatus(UserStatus.Active));
		assertThat(users.inactiveUsers()).isEqualTo(userRepository.countByStatus(UserStatus.Inactive));
		assertThat(users.activeTrainees())
				.isEqualTo(userRepository.countByRoleNameAndStatus("Trainee", UserStatus.Active));
		assertThat(users.activeTrainers())
				.isEqualTo(userRepository.countByRoleNameAndStatus("Trainer", UserStatus.Active));

		AdminDashboardResponse.ContentSummary content = dashboard.content();
		assertThat(content.totalSyllabuses()).isEqualTo(syllabusRepository.count());
		assertThat(content.activeSyllabuses()).isEqualTo(syllabusRepository.countByStatus(SyllabusStatus.Active));
		assertThat(content.pendingSyllabuses()).isEqualTo(syllabusRepository.countByStatus(SyllabusStatus.Pending));
		assertThat(content.draftingSyllabuses()).isEqualTo(syllabusRepository.countByStatus(SyllabusStatus.Drafting));
		assertThat(content.totalPrograms()).isEqualTo(trainingProgramRepository.count());
		assertThat(content.activePrograms())
				.isEqualTo(trainingProgramRepository.countByStatus(TrainingProgramStatus.Active));
		assertThat(content.totalClasses()).isEqualTo(classRepository.count());
		assertThat(content.activeClasses()).isEqualTo(classRepository.countByStatus(ClassStatus.Active));
		assertThat(content.planningClasses()).isEqualTo(classRepository.countByStatus(ClassStatus.Planning));

		AdminDashboardResponse.AssessmentSummary assessment = dashboard.assessment();
		assertThat(assessment.totalQuizzes()).isEqualTo(quizRepository.count());
		assertThat(assessment.publishedQuizzes()).isEqualTo(quizRepository.countByStatus(QuizStatus.Published));
		assertThat(assessment.submittedAttempts())
				.isEqualTo(quizAttemptRepository.countForDashboard(QuizAttemptStatus.Submitted, null, null, null));
		assertThat(assessment.passedAttempts())
				.isEqualTo(quizAttemptRepository.countForDashboard(QuizAttemptStatus.Submitted, true, null, null));
	}

	@Test
	void buildsWithinStatementBudget() {
		Measured<AdminDashboardResponse> measured = measure(() -> adminDashboardService.getDashboard());

		// 27 before the per-status counts were grouped.
		assertThat(measured.statements()).isLessThanOrEqualTo(16);
	}
}
