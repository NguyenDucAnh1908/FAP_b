package com.fap.result.service;

import com.fap.clazz.entity.ClassEnrollment;
import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassEnrollmentStatus;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.repository.ClassEnrollmentRepository;
import com.fap.clazz.repository.ClassRepository;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.i18n.MessageService;
import com.fap.notification.service.NotificationService;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.entity.QuizAttempt;
import com.fap.quiz.enums.QuizAttemptStatus;
import com.fap.quiz.enums.QuizStatus;
import com.fap.quiz.repository.QuizAssignmentRepository;
import com.fap.quiz.repository.QuizAttemptRepository;
import com.fap.quiz.repository.QuizRepository;
import com.fap.result.dto.ClassCourseResultsResponse;
import com.fap.result.dto.CompletionPolicyQuizRequest;
import com.fap.result.dto.CompletionPolicyQuizResponse;
import com.fap.result.dto.CompletionPolicyResponse;
import com.fap.result.dto.CourseResultAdjustmentResponse;
import com.fap.result.dto.CourseResultQuizResponse;
import com.fap.result.dto.CourseResultResponse;
import com.fap.result.dto.CourseResultSummaryResponse;
import com.fap.result.dto.UpdateCompletionPolicyRequest;
import com.fap.result.dto.UpdateCourseResultRequest;
import com.fap.result.entity.ClassCompletionQuiz;
import com.fap.result.entity.CourseResult;
import com.fap.result.entity.CourseResultAdjustment;
import com.fap.result.entity.CourseResultQuiz;
import com.fap.result.enums.CourseResultStatus;
import com.fap.result.repository.ClassCompletionQuizRepository;
import com.fap.result.repository.CourseResultAdjustmentRepository;
import com.fap.result.repository.CourseResultQuizRepository;
import com.fap.result.repository.CourseResultRepository;
import com.fap.training.entity.AttendanceRecord;
import com.fap.training.entity.TrainingRegistration;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.AttendanceStatus;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.repository.AttendanceRecordRepository;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fap.training.repository.TrainingSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CourseResultService {
	private static final Collection<ClassEnrollmentStatus> RESULT_ENROLLMENT_STATUSES = List.of(
			ClassEnrollmentStatus.Enrolled,
			ClassEnrollmentStatus.Completed,
			ClassEnrollmentStatus.Withdrawn);
	private static final Collection<TrainingRegistrationStatus> ATTENDANCE_REGISTRATION_STATUSES = List.of(
			TrainingRegistrationStatus.Registered,
			TrainingRegistrationStatus.Completed);

	private final ClassRepository classRepository;
	private final ClassEnrollmentRepository classEnrollmentRepository;
	private final ClassCompletionQuizRepository completionQuizRepository;
	private final CourseResultRepository courseResultRepository;
	private final CourseResultQuizRepository resultQuizRepository;
	private final CourseResultAdjustmentRepository adjustmentRepository;
	private final QuizRepository quizRepository;
	private final QuizAssignmentRepository quizAssignmentRepository;
	private final QuizAttemptRepository quizAttemptRepository;
	private final TrainingSessionRepository trainingSessionRepository;
	private final TrainingRegistrationRepository trainingRegistrationRepository;
	private final AttendanceRecordRepository attendanceRecordRepository;
	private final AuditLogService auditLogService;
	private final NotificationService notificationService;
	private final MessageService messageService;

	public CourseResultService(
			ClassRepository classRepository,
			ClassEnrollmentRepository classEnrollmentRepository,
			ClassCompletionQuizRepository completionQuizRepository,
			CourseResultRepository courseResultRepository,
			CourseResultQuizRepository resultQuizRepository,
			CourseResultAdjustmentRepository adjustmentRepository,
			QuizRepository quizRepository,
			QuizAssignmentRepository quizAssignmentRepository,
			QuizAttemptRepository quizAttemptRepository,
			TrainingSessionRepository trainingSessionRepository,
			TrainingRegistrationRepository trainingRegistrationRepository,
			AttendanceRecordRepository attendanceRecordRepository,
			AuditLogService auditLogService,
			NotificationService notificationService,
			MessageService messageService) {
		this.classRepository = classRepository;
		this.classEnrollmentRepository = classEnrollmentRepository;
		this.completionQuizRepository = completionQuizRepository;
		this.courseResultRepository = courseResultRepository;
		this.resultQuizRepository = resultQuizRepository;
		this.adjustmentRepository = adjustmentRepository;
		this.quizRepository = quizRepository;
		this.quizAssignmentRepository = quizAssignmentRepository;
		this.quizAttemptRepository = quizAttemptRepository;
		this.trainingSessionRepository = trainingSessionRepository;
		this.trainingRegistrationRepository = trainingRegistrationRepository;
		this.attendanceRecordRepository = attendanceRecordRepository;
		this.auditLogService = auditLogService;
		this.notificationService = notificationService;
		this.messageService = messageService;
	}

	@Transactional(readOnly = true)
	public CompletionPolicyResponse getPolicy(Long classId) {
		FapClass fapClass = findClass(classId);
		return toPolicyResponse(fapClass, completionQuizRepository.findByFapClassIdOrderByIdAsc(classId));
	}

	@Transactional
	public CompletionPolicyResponse updatePolicy(Long classId, UpdateCompletionPolicyRequest request, Long currentUserId) {
		FapClass fapClass = findClassForUpdate(classId);
		if (fapClass.getStatus() == ClassStatus.Closed) {
			throw new ConflictException("CLASS_COMPLETION_POLICY_LOCKED", "Closed class completion policy cannot be changed");
		}
		Set<Long> quizIds = new HashSet<>();
		for (CompletionPolicyQuizRequest item : request.requiredQuizzes()) {
			if (!quizIds.add(item.quizId())) {
				throw new BadRequestException("DUPLICATE_REQUIRED_QUIZ", "Required quizzes must be unique");
			}
			if (!quizAssignmentRepository.existsByQuizIdAndFapClassId(item.quizId(), classId)) {
				throw new BadRequestException("QUIZ_NOT_ASSIGNED_TO_CLASS", "Required quiz must be assigned directly to the class");
			}
		}

		LocalDateTime now = LocalDateTime.now();
		completionQuizRepository.deleteByFapClassId(classId);
		completionQuizRepository.flush();
		List<ClassCompletionQuiz> saved = request.requiredQuizzes().stream().map(item -> {
			Quiz quiz = quizRepository.findById(item.quizId())
					.orElseThrow(() -> new NotFoundException("Quiz not found"));
			ClassCompletionQuiz policyQuiz = new ClassCompletionQuiz();
			policyQuiz.setFapClass(fapClass);
			policyQuiz.setQuiz(quiz);
			policyQuiz.setPassingScore(item.passingScore());
			policyQuiz.setCreatedAt(now);
			policyQuiz.setUpdatedAt(now);
			policyQuiz.setCreatedBy(currentUserId);
			policyQuiz.setUpdatedBy(currentUserId);
			return completionQuizRepository.save(policyQuiz);
		}).toList();
		fapClass.setMinimumAttendanceRate(request.minimumAttendanceRate().setScale(2, RoundingMode.HALF_UP));
		fapClass.setUpdatedAt(now);
		fapClass.setUpdatedBy(currentUserId);
		auditLogService.record("UPDATE_COMPLETION_POLICY", "class", classId);
		return toPolicyResponse(fapClass, saved);
	}

	@Transactional(readOnly = true)
	public ClassCourseResultsResponse list(Long classId) {
		FapClass fapClass = findClass(classId);
		List<CourseResultResponse> results = courseResultRepository
				.findByFapClassIdOrderByClassEnrollmentUserFullNameAsc(classId).stream()
				.map(this::toResponse)
				.toList();
		return new ClassCourseResultsResponse(
				classId,
				fapClass.getName(),
				fapClass.getClassCode(),
				toSummary(results),
				results);
	}

	@Transactional(readOnly = true)
	public CourseResultResponse get(Long classId, Long userId) {
		return toResponse(findResult(classId, userId));
	}

	@Transactional(readOnly = true)
	public CourseResultResponse getMine(Long classId, Long userId) {
		CourseResult result = findResult(classId, userId);
		if (result.getPublishedAt() == null) {
			throw new ConflictException("COURSE_RESULT_NOT_PUBLISHED", "Course result has not been published");
		}
		return toResponse(result);
	}

	@Transactional
	public ClassCourseResultsResponse calculate(Long classId, Long currentUserId) {
		FapClass fapClass = findClassForUpdate(classId);
		if (fapClass.getStatus() != ClassStatus.Active) {
			throw new ConflictException("CLASS_RESULT_NOT_CALCULABLE", "Only active class results can be calculated");
		}
		calculateAll(fapClass, currentUserId);
		auditLogService.record("CALCULATE_COURSE_RESULTS", "class", classId);
		return list(classId);
	}

	@Transactional
	public void finalizeForClosure(FapClass fapClass, Long currentUserId) {
		validateSessionsForClosure(fapClass.getId());
		validateRequiredQuizzesForClosure(fapClass.getId());
		List<CourseResult> results = calculateAll(fapClass, currentUserId);
		if (results.stream().anyMatch(result -> result.effectiveStatus() == CourseResultStatus.InProgress)) {
			throw new ConflictException("COURSE_RESULTS_INCOMPLETE", "All course results must be calculated before closing the class");
		}
		auditLogService.record("FINALIZE_COURSE_RESULTS", "class", fapClass.getId());
	}

	@Transactional
	public CourseResultResponse adjust(Long classId, Long userId, UpdateCourseResultRequest request, Long currentUserId) {
		if (request.status() != CourseResultStatus.Passed && request.status() != CourseResultStatus.Failed) {
			throw new BadRequestException("INVALID_RESULT_OVERRIDE_STATUS", "Result override must be Passed or Failed");
		}
		CourseResult result = courseResultRepository.findForUpdate(classId, userId)
				.orElseThrow(() -> new NotFoundException("Course result not found"));
		if (result.getCalculatedStatus() == CourseResultStatus.InProgress
				|| result.getCalculatedStatus() == CourseResultStatus.Withdrawn) {
			throw new ConflictException("COURSE_RESULT_NOT_ADJUSTABLE", "Only calculated Passed or Failed results can be adjusted");
		}

		LocalDateTime now = LocalDateTime.now();
		CourseResultStatus previousStatus = result.effectiveStatus();
		CourseResultAdjustment adjustment = new CourseResultAdjustment();
		adjustment.setCourseResult(result);
		adjustment.setPreviousStatus(previousStatus);
		adjustment.setNewStatus(request.status());
		adjustment.setReason(request.reason().trim());
		adjustment.setAdjustedBy(currentUserId);
		adjustment.setAdjustedAt(now);
		adjustmentRepository.save(adjustment);

		result.setOverrideStatus(request.status());
		result.setOverrideReason(request.reason().trim());
		result.setOverriddenBy(currentUserId);
		result.setOverriddenAt(now);
		result.setPublishedAt(null);
		result.setPublishedBy(null);
		result.setUpdatedAt(now);
		auditLogService.record("ADJUST_COURSE_RESULT:" + request.status().name(), "course_result", result.getId());
		return toResponse(result);
	}

	@Transactional
	public ClassCourseResultsResponse publish(Long classId, Long currentUserId) {
		FapClass fapClass = findClassForUpdate(classId);
		if (fapClass.getStatus() != ClassStatus.Closed) {
			throw new ConflictException("CLASS_NOT_CLOSED", "Course results can only be published after the class is closed");
		}
		List<CourseResult> results = courseResultRepository.findByFapClassIdOrderByClassEnrollmentUserFullNameAsc(classId);
		if (results.isEmpty() || results.stream().anyMatch(result -> result.effectiveStatus() == CourseResultStatus.InProgress)) {
			throw new ConflictException("COURSE_RESULTS_INCOMPLETE", "All course results must be calculated before publication");
		}

		LocalDateTime now = LocalDateTime.now();
		List<CourseResult> unpublished = results.stream().filter(result -> result.getPublishedAt() == null).toList();
		for (CourseResult result : unpublished) {
			result.setPublishedAt(now);
			result.setPublishedBy(currentUserId);
			result.setUpdatedAt(now);
			notificationService.create(
					result.getClassEnrollment().getUser().getId(),
					messageService.get("notification.course_result.title"),
					messageService.get(
							"notification.course_result.message",
							fapClass.getName(),
							messageService.get("course_result.status." + result.effectiveStatus().name().toLowerCase())));
		}
		if (!unpublished.isEmpty()) {
			auditLogService.record("PUBLISH_COURSE_RESULTS", "class", classId);
		}
		return list(classId);
	}

	@Transactional
	public void initializeForEnrollment(ClassEnrollment enrollment, Long currentUserId) {
		CourseResult existing = courseResultRepository.findByClassEnrollmentId(enrollment.getId()).orElse(null);
		if (existing != null) {
			resultQuizRepository.deleteByCourseResultId(existing.getId());
			existing.setCalculatedStatus(CourseResultStatus.InProgress);
			existing.setOverrideStatus(null);
			existing.setOverrideReason(null);
			existing.setOverriddenAt(null);
			existing.setOverriddenBy(null);
			existing.setPublishedAt(null);
			existing.setPublishedBy(null);
			existing.setCalculatedAt(null);
			existing.setCalculatedBy(null);
			existing.setAttendanceRate(BigDecimal.ZERO.setScale(2));
			existing.setAttendedSessions(0);
			existing.setTotalSessions(0);
			existing.setRequiredQuizCount(0);
			existing.setPassedQuizCount(0);
			existing.setUpdatedAt(LocalDateTime.now());
			auditLogService.record("REOPEN_COURSE_RESULT", "course_result", existing.getId());
			return;
		}
		CourseResult result = new CourseResult();
		result.setFapClass(enrollment.getFapClass());
		result.setClassEnrollment(enrollment);
		result.setCalculatedStatus(enrollment.getStatus() == ClassEnrollmentStatus.Withdrawn
				? CourseResultStatus.Withdrawn
				: CourseResultStatus.InProgress);
		result.setUpdatedAt(LocalDateTime.now());
		CourseResult saved = courseResultRepository.save(result);
		auditLogService.record("INITIALIZE_COURSE_RESULT", "course_result", saved.getId());
	}

	@Transactional
	public void markWithdrawn(ClassEnrollment enrollment, Long currentUserId) {
		courseResultRepository.findByClassEnrollmentId(enrollment.getId()).ifPresent(result -> {
			result.setCalculatedStatus(CourseResultStatus.Withdrawn);
			result.setOverrideStatus(null);
			result.setOverrideReason(null);
			result.setOverriddenAt(null);
			result.setOverriddenBy(null);
			result.setPublishedAt(null);
			result.setPublishedBy(null);
			result.setCalculatedAt(LocalDateTime.now());
			result.setCalculatedBy(currentUserId);
			result.setUpdatedAt(LocalDateTime.now());
			auditLogService.record("WITHDRAW_COURSE_RESULT", "course_result", result.getId());
		});
	}

	private List<CourseResult> calculateAll(FapClass fapClass, Long currentUserId) {
		List<ClassCompletionQuiz> requiredQuizzes = completionQuizRepository
				.findByFapClassIdOrderByIdAsc(fapClass.getId());
		List<ClassEnrollment> enrollments = classEnrollmentRepository
				.findByFapClassIdAndStatusInOrderByCreatedAtAscIdAsc(fapClass.getId(), RESULT_ENROLLMENT_STATUSES);
		return enrollments.stream()
				.map(enrollment -> calculateOne(fapClass, enrollment, requiredQuizzes, currentUserId))
				.toList();
	}

	private CourseResult calculateOne(
			FapClass fapClass,
			ClassEnrollment enrollment,
			List<ClassCompletionQuiz> requiredQuizzes,
			Long currentUserId) {
		CourseResult result = courseResultRepository.findByClassEnrollmentId(enrollment.getId()).orElseGet(() -> {
			CourseResult created = new CourseResult();
			created.setFapClass(fapClass);
			created.setClassEnrollment(enrollment);
			created.setUpdatedAt(LocalDateTime.now());
			return courseResultRepository.save(created);
		});

		if (enrollment.getStatus() == ClassEnrollmentStatus.Withdrawn) {
			result.setCalculatedStatus(CourseResultStatus.Withdrawn);
			result.setOverrideStatus(null);
			result.setCalculatedAt(LocalDateTime.now());
			result.setCalculatedBy(currentUserId);
			result.setUpdatedAt(LocalDateTime.now());
			return result;
		}

		List<TrainingRegistration> registrations = trainingRegistrationRepository.findMineByClassId(
				enrollment.getUser().getId(), fapClass.getId(), ATTENDANCE_REGISTRATION_STATUSES);
		Set<Long> completedSessionIds = new HashSet<>();
		for (TrainingRegistration registration : registrations) {
			if (registration.getTrainingSession().getStatus() == TrainingSessionStatus.Completed) {
				completedSessionIds.add(registration.getTrainingSession().getId());
			}
		}
		Map<Long, AttendanceStatus> attendanceBySession = new HashMap<>();
		for (AttendanceRecord attendance : attendanceRecordRepository
				.findByTrainingSessionFapClassIdAndUserId(fapClass.getId(), enrollment.getUser().getId())) {
			attendanceBySession.put(attendance.getTrainingSession().getId(), attendance.getStatus());
		}
		int attendedSessions = (int) completedSessionIds.stream()
				.map(attendanceBySession::get)
				.filter(status -> status == AttendanceStatus.Present || status == AttendanceStatus.Late)
				.count();
		int totalSessions = completedSessionIds.size();
		BigDecimal attendanceRate = totalSessions == 0
				? BigDecimal.ZERO.setScale(2)
				: BigDecimal.valueOf(attendedSessions)
						.multiply(BigDecimal.valueOf(100))
						.divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP);

		resultQuizRepository.deleteByCourseResultId(result.getId());
		resultQuizRepository.flush();
		int passedQuizCount = 0;
		for (ClassCompletionQuiz requiredQuiz : requiredQuizzes) {
			QuizAttempt attempt = quizAttemptRepository
					.findFirstByQuizIdAndUserIdAndStatusOrderByScoreDescIdDesc(
							requiredQuiz.getQuiz().getId(), enrollment.getUser().getId(), QuizAttemptStatus.Submitted)
					.orElse(null);
			boolean passed = attempt != null && attempt.getScore() != null
					&& attempt.getScore() >= requiredQuiz.getPassingScore();
			CourseResultQuiz snapshot = new CourseResultQuiz();
			snapshot.setCourseResult(result);
			snapshot.setQuiz(requiredQuiz.getQuiz());
			snapshot.setRequiredScore(requiredQuiz.getPassingScore());
			snapshot.setBestAttemptId(attempt == null ? null : attempt.getId());
			snapshot.setBestScore(attempt == null ? null : attempt.getScore());
			snapshot.setPassed(passed);
			resultQuizRepository.save(snapshot);
			if (passed) {
				passedQuizCount++;
			}
		}

		boolean attendancePassed = totalSessions > 0
				&& attendanceRate.compareTo(fapClass.getMinimumAttendanceRate()) >= 0;
		boolean quizzesPassed = passedQuizCount == requiredQuizzes.size();
		result.setAttendanceRate(attendanceRate);
		result.setAttendedSessions(attendedSessions);
		result.setTotalSessions(totalSessions);
		result.setRequiredQuizCount(requiredQuizzes.size());
		result.setPassedQuizCount(passedQuizCount);
		result.setCalculatedStatus(attendancePassed && quizzesPassed
				? CourseResultStatus.Passed
				: CourseResultStatus.Failed);
		result.setCalculatedAt(LocalDateTime.now());
		result.setCalculatedBy(currentUserId);
		result.setPublishedAt(null);
		result.setPublishedBy(null);
		result.setUpdatedAt(LocalDateTime.now());
		return result;
	}

	private void validateSessionsForClosure(Long classId) {
		List<TrainingSession> sessions = trainingSessionRepository.findByFapClassIdOrderBySessionDateAscStartTimeAsc(classId);
		if (sessions.stream().noneMatch(session -> session.getStatus() == TrainingSessionStatus.Completed)) {
			throw new ConflictException("CLASS_COMPLETED_SESSION_REQUIRED", "Class requires at least one completed session before closing");
		}
		if (sessions.stream().anyMatch(session -> session.getStatus() == TrainingSessionStatus.Upcoming)) {
			throw new ConflictException("CLASS_SESSIONS_INCOMPLETE", "All class sessions must be completed or canceled before closing");
		}
	}

	private void validateRequiredQuizzesForClosure(Long classId) {
		completionQuizRepository.findByFapClassIdOrderByIdAsc(classId).forEach(item -> {
			if (item.getQuiz().getStatus() != QuizStatus.Closed) {
				throw new ConflictException("REQUIRED_QUIZ_NOT_CLOSED", "All required quizzes must be closed before closing the class");
			}
		});
	}

	private CompletionPolicyResponse toPolicyResponse(FapClass fapClass, List<ClassCompletionQuiz> quizzes) {
		return new CompletionPolicyResponse(
				fapClass.getId(),
				fapClass.getMinimumAttendanceRate(),
				quizzes.stream().map(item -> new CompletionPolicyQuizResponse(
						item.getQuiz().getId(),
						item.getQuiz().getTitle(),
						item.getPassingScore(),
						item.getQuiz().getStatus())).toList());
	}

	private CourseResultResponse toResponse(CourseResult result) {
		List<CourseResultQuizResponse> quizzes = resultQuizRepository
				.findByCourseResultIdOrderByIdAsc(result.getId()).stream()
				.map(item -> new CourseResultQuizResponse(
						item.getQuiz().getId(),
						item.getQuiz().getTitle(),
						item.getRequiredScore(),
						item.getBestAttemptId(),
						item.getBestScore(),
						item.isPassed()))
				.toList();
		List<CourseResultAdjustmentResponse> adjustments = adjustmentRepository
				.findByCourseResultIdOrderByAdjustedAtDescIdDesc(result.getId()).stream()
				.map(item -> new CourseResultAdjustmentResponse(
						item.getId(), item.getPreviousStatus(), item.getNewStatus(), item.getReason(),
						item.getAdjustedBy(), item.getAdjustedAt()))
				.toList();
		return new CourseResultResponse(
				result.getId(),
				result.getFapClass().getId(),
				result.getClassEnrollment().getId(),
				result.getClassEnrollment().getUser().getId(),
				result.getClassEnrollment().getUser().getFullName(),
				result.getClassEnrollment().getUser().getEmail(),
				result.effectiveStatus(),
				result.getCalculatedStatus(),
				result.getOverrideStatus(),
				result.getAttendanceRate(),
				result.getAttendedSessions(),
				result.getTotalSessions(),
				result.getRequiredQuizCount(),
				result.getPassedQuizCount(),
				result.getOverrideReason(),
				result.getOverriddenBy(),
				result.getOverriddenAt(),
				result.getPublishedAt() != null,
				result.getPublishedAt(),
				result.getCalculatedAt(),
				result.getVersionNo(),
				quizzes,
				adjustments);
	}

	private CourseResultSummaryResponse toSummary(List<CourseResultResponse> results) {
		return new CourseResultSummaryResponse(
				results.size(),
				results.stream().filter(item -> item.status() == CourseResultStatus.InProgress).count(),
				results.stream().filter(item -> item.status() == CourseResultStatus.Passed).count(),
				results.stream().filter(item -> item.status() == CourseResultStatus.Failed).count(),
				results.stream().filter(item -> item.status() == CourseResultStatus.Withdrawn).count(),
				results.stream().filter(CourseResultResponse::published).count());
	}

	private FapClass findClass(Long classId) {
		return classRepository.findWithTrainingProgramById(classId)
				.orElseThrow(() -> new NotFoundException("Class not found"));
	}

	private FapClass findClassForUpdate(Long classId) {
		return classRepository.findWithTrainingProgramByIdForUpdate(classId)
				.orElseThrow(() -> new NotFoundException("Class not found"));
	}

	private CourseResult findResult(Long classId, Long userId) {
		return courseResultRepository.findByFapClassIdAndClassEnrollmentUserId(classId, userId)
				.orElseThrow(() -> new NotFoundException("Course result not found"));
	}
}
