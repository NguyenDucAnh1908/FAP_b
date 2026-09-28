package com.fap.dashboard.service;

import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.repository.ClassRepository;
import com.fap.common.audit.AuditLogRepository;
import com.fap.common.security.RoleNames;
import com.fap.dashboard.dto.AdminDashboardResponse;
import com.fap.dashboard.dto.TrainingAnalyticsResponse;
import com.fap.program.enums.TrainingProgramStatus;
import com.fap.program.repository.TrainingProgramRepository;
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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {

	private static final String CACHE_KEY = "admin";

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
	private final Cache<String, AdminDashboardResponse> cache;
	private final TransactionTemplate readOnlyTransaction;
	private final Clock clock;

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
			TrainingAnalyticsService trainingAnalyticsService,
			PlatformTransactionManager transactionManager,
			@Value("${app.dashboard.cache-ttl:30s}") Duration cacheTtl,
			Clock clock) {
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
		// The dashboard is the same for every admin and each build runs about 15 aggregate queries,
		// so it is shared for a short TTL. generatedAt in the response shows how fresh it is.
		this.cache = Caffeine.newBuilder().maximumSize(1).expireAfterWrite(cacheTtl).build();
		this.readOnlyTransaction = new TransactionTemplate(transactionManager);
		this.readOnlyTransaction.setReadOnly(true);
		this.clock = clock;
	}

	/**
	 * Not {@code @Transactional}: a cache hit must not borrow a pooled connection. Only a miss opens
	 * a read-only transaction, which the lazy associations read by the mappers need.
	 */
	public AdminDashboardResponse getDashboard() {
		return cache.get(CACHE_KEY, key -> readOnlyTransaction.execute(status -> buildDashboard()));
	}

	private AdminDashboardResponse buildDashboard() {
		TrainingAnalyticsResponse training = trainingAnalyticsService.getAnalytics(null, null, null);
		QuizAttemptRepository.SubmittedOutcomeCount attempts = quizAttemptRepository.countSubmittedOutcomes();
		long submittedAttempts = attempts.getSubmitted();
		long passedAttempts = attempts.getPassed();

		List<TrainingSessionResponse> nextSessions = trainingSessionRepository.search(
				TrainingSessionStatus.Upcoming,
				null,
				null,
				LocalDate.now(clock),
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

		Map<UserStatus, Long> users = countsBy(userRepository.countGroupedByStatus(),
				UserRepository.StatusCount::getStatus, UserRepository.StatusCount::getTotal);
		Map<String, Long> activeUsersByRole = countsBy(
				userRepository.countByRoleNamesAndStatus(
						List.of(RoleNames.TRAINEE, RoleNames.TRAINER), UserStatus.Active),
				UserRepository.RoleCount::getRoleName, UserRepository.RoleCount::getTotal);
		Map<SyllabusStatus, Long> syllabuses = countsBy(syllabusRepository.countGroupedByStatus(),
				SyllabusRepository.StatusCount::getStatus, SyllabusRepository.StatusCount::getTotal);
		Map<TrainingProgramStatus, Long> programs = countsBy(trainingProgramRepository.countGroupedByStatus(),
				TrainingProgramRepository.StatusCount::getStatus, TrainingProgramRepository.StatusCount::getTotal);
		Map<ClassStatus, Long> classes = countsBy(classRepository.countGroupedByStatus(),
				ClassRepository.StatusCount::getStatus, ClassRepository.StatusCount::getTotal);
		Map<QuizStatus, Long> quizzes = countsBy(quizRepository.countGroupedByStatus(),
				QuizRepository.StatusCount::getStatus, QuizRepository.StatusCount::getTotal);

		return new AdminDashboardResponse(
				new AdminDashboardResponse.UserSummary(
						total(users),
						users.getOrDefault(UserStatus.Active, 0L),
						users.getOrDefault(UserStatus.Inactive, 0L),
						activeUsersByRole.getOrDefault(RoleNames.TRAINEE, 0L),
						activeUsersByRole.getOrDefault(RoleNames.TRAINER, 0L)),
				new AdminDashboardResponse.ContentSummary(
						total(syllabuses),
						syllabuses.getOrDefault(SyllabusStatus.Active, 0L),
						syllabuses.getOrDefault(SyllabusStatus.Pending, 0L),
						syllabuses.getOrDefault(SyllabusStatus.Drafting, 0L),
						total(programs),
						programs.getOrDefault(TrainingProgramStatus.Active, 0L),
						total(classes),
						classes.getOrDefault(ClassStatus.Active, 0L),
						classes.getOrDefault(ClassStatus.Planning, 0L)),
				training,
				new AdminDashboardResponse.AssessmentSummary(
						total(quizzes),
						quizzes.getOrDefault(QuizStatus.Published, 0L),
						submittedAttempts,
						passedAttempts,
						percentage(passedAttempts, submittedAttempts)),
				nextSessions,
				recentActivities,
				LocalDateTime.now(clock));
	}

	private static <T, K> Map<K, Long> countsBy(List<T> rows, Function<T, K> key, Function<T, Long> total) {
		return rows.stream().collect(Collectors.toMap(key, total));
	}

	private static long total(Map<?, Long> counts) {
		return counts.values().stream().mapToLong(Long::longValue).sum();
	}

	private double percentage(long numerator, long denominator) {
		return denominator == 0 ? 0 : Math.round(numerator * 1000.0 / denominator) / 10.0;
	}
}
