package com.fap.dashboard.service;

import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.repository.ClassRepository;
import com.fap.common.audit.AuditLogRepository;
import com.fap.dashboard.dto.AdminDashboardResponse;
import com.fap.dashboard.dto.TrainingAnalyticsResponse;
import com.fap.program.enums.TrainingProgramStatus;
import com.fap.program.repository.TrainingProgramRepository;
import com.fap.quiz.enums.QuizAttemptStatus;
import com.fap.quiz.enums.QuizStatus;
import com.fap.quiz.repository.QuizAttemptRepository;
import com.fap.quiz.repository.QuizRepository;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.repository.SyllabusRepository;
import com.fap.training.dto.TrainingSessionResponse;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.mapper.TrainingSessionMapper;
import com.fap.training.repository.TrainingSessionRepository;
import com.fap.user.enums.UserStatus;
import com.fap.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminDashboardService {

	private static final String TRAINEE_ROLE = "Trainee";
	private static final String TRAINER_ROLE = "Trainer";

	private final UserRepository userRepository;
	private final SyllabusRepository syllabusRepository;
	private final TrainingProgramRepository trainingProgramRepository;
	private final ClassRepository classRepository;
	private final QuizRepository quizRepository;
	private final QuizAttemptRepository quizAttemptRepository;
	private final TrainingSessionRepository trainingSessionRepository;
	private final TrainingSessionMapper trainingSessionMapper;
	private final AuditLogRepository auditLogRepository;
	private final TrainingAnalyticsService trainingAnalyticsService;

	public AdminDashboardService(
			UserRepository userRepository,
			SyllabusRepository syllabusRepository,
			TrainingProgramRepository trainingProgramRepository,
			ClassRepository classRepository,
			QuizRepository quizRepository,
			QuizAttemptRepository quizAttemptRepository,
			TrainingSessionRepository trainingSessionRepository,
			TrainingSessionMapper trainingSessionMapper,
			AuditLogRepository auditLogRepository,
			TrainingAnalyticsService trainingAnalyticsService) {
		this.userRepository = userRepository;
		this.syllabusRepository = syllabusRepository;
		this.trainingProgramRepository = trainingProgramRepository;
		this.classRepository = classRepository;
		this.quizRepository = quizRepository;
		this.quizAttemptRepository = quizAttemptRepository;
		this.trainingSessionRepository = trainingSessionRepository;
		this.trainingSessionMapper = trainingSessionMapper;
		this.auditLogRepository = auditLogRepository;
		this.trainingAnalyticsService = trainingAnalyticsService;
	}

	@Transactional(readOnly = true)
	public AdminDashboardResponse getDashboard() {
		TrainingAnalyticsResponse training = trainingAnalyticsService.getAnalytics(null, null, null);
		long submittedAttempts = quizAttemptRepository.countForDashboard(
				QuizAttemptStatus.Submitted, null, null, null);
		long passedAttempts = quizAttemptRepository.countForDashboard(
				QuizAttemptStatus.Submitted, true, null, null);

		List<TrainingSessionResponse> nextSessions = trainingSessionRepository.search(
				TrainingSessionStatus.Upcoming,
				null,
				null,
				LocalDate.now(),
				null,
				null,
				PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "sessionDate", "startTime", "id")))
				.map(trainingSessionMapper::toResponse)
				.getContent();

		List<AdminDashboardResponse.RecentActivity> recentActivities = auditLogRepository
				.search(null, null, null,
						PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
				.map(log -> new AdminDashboardResponse.RecentActivity(
						log.getId(),
						log.getAction(),
						log.getEntityType(),
						log.getEntityId(),
						log.getCreatedAt()))
				.getContent();

		return new AdminDashboardResponse(
				new AdminDashboardResponse.UserSummary(
						userRepository.count(),
						userRepository.countByStatus(UserStatus.Active),
						userRepository.countByStatus(UserStatus.Inactive),
						userRepository.countByRoleNameAndStatus(TRAINEE_ROLE, UserStatus.Active),
						userRepository.countByRoleNameAndStatus(TRAINER_ROLE, UserStatus.Active)),
				new AdminDashboardResponse.ContentSummary(
						syllabusRepository.count(),
						syllabusRepository.countByStatus(SyllabusStatus.Active),
						syllabusRepository.countByStatus(SyllabusStatus.Pending),
						syllabusRepository.countByStatus(SyllabusStatus.Drafting),
						trainingProgramRepository.count(),
						trainingProgramRepository.countByStatus(TrainingProgramStatus.Active),
						classRepository.count(),
						classRepository.countByStatus(ClassStatus.Active),
						classRepository.countByStatus(ClassStatus.Planning)),
				training,
				new AdminDashboardResponse.AssessmentSummary(
						quizRepository.count(),
						quizRepository.countByStatus(QuizStatus.Published),
						submittedAttempts,
						passedAttempts,
						percentage(passedAttempts, submittedAttempts)),
				nextSessions,
				recentActivities,
				LocalDateTime.now());
	}

	private double percentage(long numerator, long denominator) {
		return denominator == 0 ? 0 : Math.round(numerator * 1000.0 / denominator) / 10.0;
	}
}
