package com.fap.dashboard.service;

import com.fap.clazz.repository.ClassRepository;
import com.fap.common.audit.AuditLogRepository;
import com.fap.common.security.RoleNames;
import com.fap.dashboard.dto.AdminDashboardResponse;
import com.fap.program.repository.TrainingProgramRepository;
import com.fap.quiz.repository.QuizAttemptRepository;
import com.fap.quiz.repository.QuizRepository;
import com.fap.syllabus.repository.SyllabusRepository;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.mapper.TrainingSessionMapper;
import com.fap.training.repository.TrainingSessionRepository;
import com.fap.user.enums.UserStatus;
import com.fap.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * "Today" for the upcoming-session list and the generatedAt stamp come from the injected clock, the
 * role breakdown is keyed by the seeded role names that the query groups by, and one built dashboard
 * is shared for the cache TTL so a repeat call neither queries nor opens a transaction.
 */
class AdminDashboardServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-15T09:00:00Z"), ZoneOffset.UTC);

	private final UserRepository userRepository = mock(UserRepository.class);
	private final SyllabusRepository syllabusRepository = mock(SyllabusRepository.class);
	private final TrainingProgramRepository trainingProgramRepository = mock(TrainingProgramRepository.class);
	private final ClassRepository classRepository = mock(ClassRepository.class);
	private final QuizRepository quizRepository = mock(QuizRepository.class);
	private final QuizAttemptRepository quizAttemptRepository = mock(QuizAttemptRepository.class);
	private final TrainingSessionRepository trainingSessionRepository = mock(TrainingSessionRepository.class);
	private final TrainingSessionMapper trainingSessionMapper = mock(TrainingSessionMapper.class);
	private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
	private final TrainingAnalyticsService trainingAnalyticsService = mock(TrainingAnalyticsService.class);
	private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);

	private final AdminDashboardService service = new AdminDashboardService(
			userRepository,
			syllabusRepository,
			trainingProgramRepository,
			classRepository,
			quizRepository,
			quizAttemptRepository,
			trainingSessionRepository,
			trainingSessionMapper,
			auditLogRepository,
			trainingAnalyticsService,
			transactionManager,
			Duration.ofSeconds(30),
			CLOCK);

	@BeforeEach
	void setUp() {
		// The service unboxes both outcome totals, so the projection must not return null.
		QuizAttemptRepository.SubmittedOutcomeCount outcomes = mock(QuizAttemptRepository.SubmittedOutcomeCount.class);
		when(outcomes.getSubmitted()).thenReturn(0L);
		when(outcomes.getPassed()).thenReturn(0L);
		when(quizAttemptRepository.countSubmittedOutcomes()).thenReturn(outcomes);
		when(trainingSessionRepository.search(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(Page.empty());
		when(auditLogRepository.search(any(), any(), any(), any())).thenReturn(Page.empty());
	}

	@Test
	void takesTodayAndGeneratedAtFromTheClock() {
		AdminDashboardResponse response = service.getDashboard();

		verify(trainingSessionRepository).search(
				TrainingSessionStatus.Upcoming,
				null,
				null,
				LocalDate.now(CLOCK),
				null,
				null,
				PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "sessionDate", "startTime", "id")));
		assertThat(response.generatedAt()).isEqualTo(LocalDateTime.now(CLOCK));
	}

	@Test
	void countsActiveTraineesAndTrainersByRoleName() {
		UserRepository.RoleCount trainees = roleCount(RoleNames.TRAINEE, 3);
		UserRepository.RoleCount trainers = roleCount(RoleNames.TRAINER, 1);
		when(userRepository.countByRoleNamesAndStatus(
				List.of(RoleNames.TRAINEE, RoleNames.TRAINER), UserStatus.Active))
				.thenReturn(List.of(trainees, trainers));

		AdminDashboardResponse.UserSummary users = service.getDashboard().users();

		assertThat(users.activeTrainees()).isEqualTo(3);
		assertThat(users.activeTrainers()).isEqualTo(1);
	}

	@Test
	void servesRepeatCallsFromTheCacheBuiltInOneReadOnlyTransaction() {
		AdminDashboardResponse first = service.getDashboard();
		AdminDashboardResponse second = service.getDashboard();

		assertThat(second).isSameAs(first);
		ArgumentCaptor<TransactionDefinition> definition = ArgumentCaptor.forClass(TransactionDefinition.class);
		verify(transactionManager).getTransaction(definition.capture());
		assertThat(definition.getValue().isReadOnly()).isTrue();
		verify(userRepository).countGroupedByStatus();
	}

	private UserRepository.RoleCount roleCount(String roleName, long total) {
		UserRepository.RoleCount item = mock(UserRepository.RoleCount.class);
		when(item.getRoleName()).thenReturn(roleName);
		when(item.getTotal()).thenReturn(total);
		return item;
	}
}
