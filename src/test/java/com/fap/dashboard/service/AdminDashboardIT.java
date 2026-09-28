package com.fap.dashboard.service;

import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.repository.ClassRepository;
import com.fap.common.audit.AuditLog;
import com.fap.common.audit.AuditLogRepository;
import com.fap.common.security.RoleNames;
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
import com.fap.training.dto.TrainingSessionResponse;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.mapper.TrainingSessionMapper;
import com.fap.training.repository.TrainingSessionRepository;
import com.fap.user.enums.UserStatus;
import com.fap.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

	@Autowired
	private TrainingSessionRepository trainingSessionRepository;

	@Autowired
	private TrainingSessionMapper trainingSessionMapper;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Test
	void groupedCountsMatchPerStatusCounts() {
		AdminDashboardResponse dashboard = adminDashboardService.getDashboard();

		AdminDashboardResponse.UserSummary users = dashboard.users();
		assertThat(users.totalUsers()).isEqualTo(userRepository.count());
		assertThat(users.activeUsers()).isEqualTo(userRepository.countByStatus(UserStatus.Active));
		assertThat(users.inactiveUsers()).isEqualTo(userRepository.countByStatus(UserStatus.Inactive));
		assertThat(users.activeTrainees())
				.isEqualTo(userRepository.countByRoleNameAndStatus(RoleNames.TRAINEE, UserStatus.Active));
		assertThat(users.activeTrainers())
				.isEqualTo(userRepository.countByRoleNameAndStatus(RoleNames.TRAINER, UserStatus.Active));

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

	@ParameterizedTest
	@ValueSource(ints = {0, 3, 5, 8})
	void upcomingPreviewIsBoundedOrderedAndUsesOneStatement(int rowCount) {
		LocalDate fromDate = afterLastSession();
		List<TrainingSession> sessions = addUpcomingSessions(rowCount, fromDate);
		TrainingSession template = sessionTemplate();
		addSession(template, fromDate.minusDays(1).atStartOfDay(), TrainingSessionStatus.Upcoming);
		addSession(template, fromDate.atStartOfDay(), TrainingSessionStatus.Completed);
		addSession(template, fromDate.atStartOfDay(), TrainingSessionStatus.Canceled);
		TrainingSession deleted = addSession(template, fromDate.atStartOfDay(), TrainingSessionStatus.Upcoming);
		deleted.setDeleted(true);
		deleted.setDeletedAt(LocalDateTime.now());

		Measured<List<TrainingSessionResponse>> measured = measure(() -> trainingSessionRepository
				.findForDashboard(TrainingSessionStatus.Upcoming, fromDate,
						PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "sessionDate", "startTime", "id")))
				.stream().map(trainingSessionMapper::toResponse).toList());

		assertThat(measured.result()).extracting(TrainingSessionResponse::id)
				.containsExactlyElementsOf(sessions.stream().limit(5).map(TrainingSession::getId).toList());
		assertThat(measured.statements()).isEqualTo(1);
		// Includes the class and trainer needed by the mapper, not every matching session.
		assertThat(measured.entitiesLoaded()).isLessThanOrEqualTo(Math.min(rowCount, 5) + 2);
	}

	@Test
	void recentActivityPreviewIsBoundedOrderedAndUsesOneStatement() {
		List<AuditLog> logs = addRecentActivities();

		Measured<List<AuditLog>> measured = measure(() -> auditLogRepository.findAllBy(
				PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt", "id"))));

		assertThat(measured.result()).extracting(AuditLog::getId)
				.containsExactlyElementsOf(logs.stream().limit(5).map(AuditLog::getId).toList());
		assertThat(measured.statements()).isEqualTo(1);
		assertThat(measured.entitiesLoaded()).isEqualTo(5);
	}

	@Test
	void fullPreviewsStayWithinTheDashboardStatementBudget() {
		addUpcomingSessions(8, afterLastSession());
		List<AuditLog> logs = addRecentActivities();

		Measured<AdminDashboardResponse> measured = measure(() -> adminDashboardService.getDashboard());

		assertThat(measured.result().nextSessions()).hasSize(5);
		assertThat(measured.result().recentActivities()).extracting(AdminDashboardResponse.RecentActivity::id)
				.containsExactlyElementsOf(logs.stream().limit(5).map(AuditLog::getId).toList());
		assertThat(measured.statements()).isLessThanOrEqualTo(16);
	}

	private LocalDate afterLastSession() {
		LocalDate lastDate = entityManager.createQuery(
				"select max(s.sessionDate) from TrainingSession s", LocalDate.class).getSingleResult();
		return (lastDate == null || lastDate.isBefore(LocalDate.now()) ? LocalDate.now() : lastDate).plusDays(1);
	}

	private TrainingSession sessionTemplate() {
		return entityManager.createQuery(
				"select s from TrainingSession s join fetch s.fapClass join fetch s.trainer order by s.id",
				TrainingSession.class).setMaxResults(1).getSingleResult();
	}

	private List<TrainingSession> addUpcomingSessions(int rowCount, LocalDate fromDate) {
		TrainingSession template = sessionTemplate();
		List<TrainingSession> sessions = new ArrayList<>();
		// Insert in reverse date order, including ties, to exercise every sort key.
		for (int i = rowCount - 1; i >= 0; i--) {
			LocalDateTime start = fromDate.plusDays(i / 4).atTime(9 + (i % 4) / 2, 0);
			sessions.add(addSession(template, start, TrainingSessionStatus.Upcoming));
		}
		sessions.sort(Comparator.comparing(TrainingSession::getSessionDate)
				.thenComparing(TrainingSession::getStartTime).thenComparing(TrainingSession::getId));
		return sessions;
	}

	private TrainingSession addSession(TrainingSession template, LocalDateTime start, TrainingSessionStatus status) {
		TrainingSession session = new TrainingSession();
		session.setFapClass(template.getFapClass());
		session.setTrainer(template.getTrainer());
		session.setTitle("Dashboard preview regression");
		session.setSessionDate(start.toLocalDate());
		session.setStartTime(start);
		session.setEndTime(start.plusHours(1));
		session.setSessionType(template.getSessionType());
		session.setStatus(status);
		session.setCreatedAt(LocalDateTime.now());
		session.setUpdatedAt(LocalDateTime.now());
		entityManager.persist(session);
		return session;
	}

	private List<AuditLog> addRecentActivities() {
		LocalDateTime latest = entityManager.createQuery(
				"select max(a.createdAt) from AuditLog a", LocalDateTime.class).getSingleResult();
		LocalDateTime start = (latest == null ? LocalDateTime.now() : latest).plusDays(1);
		List<AuditLog> logs = new ArrayList<>();
		for (int i = 7; i >= 0; i--) {
			AuditLog log = new AuditLog();
			log.setAction("Dashboard preview regression");
			log.setEntityType("Test");
			log.setCreatedAt(start.plusSeconds(i / 2));
			entityManager.persist(log);
			logs.add(log);
		}
		logs.sort(Comparator.comparing(AuditLog::getCreatedAt).thenComparing(AuditLog::getId).reversed());
		return logs;
	}
}
