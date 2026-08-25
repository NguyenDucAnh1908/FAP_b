package com.fap.training.service;

import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.repository.ClassRepository;
import com.fap.clazz.repository.ClassTrainerRepository;
import com.fap.clazz.service.ClassEnrollmentService;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.notification.service.NotificationService;
import com.fap.training.dto.CreateTrainingSessionRequest;
import com.fap.training.dto.UpdateTrainingSessionRequest;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.TrainingRegistrationMode;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.enums.TrainingSessionType;
import com.fap.training.mapper.TrainingSessionMapper;
import com.fap.training.repository.AttendanceRecordRepository;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fap.training.repository.TrainingSessionRepository;
import com.fap.user.entity.User;
import com.fap.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingSessionSchedulingTest {

	private static final Long CLASS_ID = 10L;
	private static final Long SESSION_ID = 20L;
	private static final Long TRAINER_ID = 30L;
	private static final Long ACTOR_ID = 40L;
	private static final LocalDate SESSION_DATE = LocalDate.of(2026, 8, 20);
	private static final LocalDateTime START_TIME = SESSION_DATE.atTime(9, 0);
	private static final LocalDateTime END_TIME = SESSION_DATE.atTime(11, 0);

	@Mock private TrainingSessionRepository trainingSessionRepository;
	@Mock private TrainingRegistrationRepository trainingRegistrationRepository;
	@Mock private AttendanceRecordRepository attendanceRecordRepository;
	@Mock private ClassRepository classRepository;
	@Mock private ClassTrainerRepository classTrainerRepository;
	@Mock private UserRepository userRepository;
	@Mock private TrainingSessionMapper trainingSessionMapper;
	@Mock private AuditLogService auditLogService;
	@Mock private NotificationService notificationService;
	@Mock private ClassEnrollmentService classEnrollmentService;

	private TrainingSessionService service;
	private FapClass fapClass;
	private User trainer;

	@BeforeEach
	void setUp() {
		service = new TrainingSessionService(
				trainingSessionRepository,
				trainingRegistrationRepository,
				attendanceRecordRepository,
				classRepository,
				classTrainerRepository,
				userRepository,
				trainingSessionMapper,
				auditLogService,
				notificationService,
				classEnrollmentService);
		fapClass = new FapClass();
		fapClass.setId(CLASS_ID);
		fapClass.setStatus(ClassStatus.Active);
		fapClass.setStartDate(SESSION_DATE.minusDays(1));
		fapClass.setEndDate(SESSION_DATE.plusDays(1));
		trainer = new User();
		trainer.setId(TRAINER_ID);
	}

	@Test
	void rejectsOverlappingSessionInSameClass() {
		givenActiveClassAndAssignedTrainer();
		when(trainingSessionRepository.countClassScheduleConflicts(
				null, CLASS_ID, START_TIME, END_TIME, TrainingSessionStatus.Canceled)).thenReturn(1L);

		assertThatThrownBy(() -> service.create(createRequest(), ACTOR_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("TRAINING_SESSION_CLASS_SCHEDULE_CONFLICT");
	}

	@Test
	void rejectsOverlappingSessionForTrainerAcrossClasses() {
		givenActiveClassAndAssignedTrainer();
		when(trainingSessionRepository.countTrainerScheduleConflicts(
				null, TRAINER_ID, START_TIME, END_TIME, TrainingSessionStatus.Canceled)).thenReturn(1L);

		assertThatThrownBy(() -> service.create(createRequest(), ACTOR_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("TRAINING_SESSION_TRAINER_SCHEDULE_CONFLICT");
	}

	@Test
	void rejectsCapacityBelowCurrentEnrollment() {
		TrainingSession session = new TrainingSession();
		session.setId(SESSION_ID);
		session.setFapClass(fapClass);
		session.setStatus(TrainingSessionStatus.Upcoming);
		session.setEnrolledCount(12);
		when(trainingSessionRepository.findWithClassAndTrainerById(SESSION_ID)).thenReturn(Optional.of(session));
		when(classTrainerRepository.existsByFapClassIdAndUserId(CLASS_ID, TRAINER_ID)).thenReturn(true);
		when(userRepository.findById(TRAINER_ID)).thenReturn(Optional.of(trainer));

		assertThatThrownBy(() -> service.update(SESSION_ID, updateRequest(10), ACTOR_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("TRAINING_SESSION_CAPACITY_BELOW_ENROLLED");
	}

	private void givenActiveClassAndAssignedTrainer() {
		when(classRepository.findWithTrainingProgramById(CLASS_ID)).thenReturn(Optional.of(fapClass));
		when(classTrainerRepository.existsByFapClassIdAndUserId(CLASS_ID, TRAINER_ID)).thenReturn(true);
		when(userRepository.findById(TRAINER_ID)).thenReturn(Optional.of(trainer));
	}

	private CreateTrainingSessionRequest createRequest() {
		return new CreateTrainingSessionRequest(
				CLASS_ID,
				"Spring Boot",
				null,
				TRAINER_ID,
				"A101",
				SESSION_DATE,
				START_TIME,
				END_TIME,
				TrainingSessionType.Offline,
				null,
				30,
				TrainingRegistrationMode.SelfEnroll);
	}

	private UpdateTrainingSessionRequest updateRequest(int capacity) {
		return new UpdateTrainingSessionRequest(
				"Spring Boot",
				null,
				TRAINER_ID,
				"A101",
				SESSION_DATE,
				START_TIME,
				END_TIME,
				TrainingSessionType.Offline,
				null,
				capacity,
				TrainingRegistrationMode.SelfEnroll);
	}
}
