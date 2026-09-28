package com.fap.result.service;

import com.fap.clazz.entity.ClassEnrollment;
import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassEnrollmentStatus;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.repository.ClassRepository;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.i18n.MessageService;
import com.fap.notification.service.NotificationService;
import com.fap.quiz.enums.QuizStatus;
import com.fap.result.dto.ClassCourseResultsResponse;
import com.fap.result.dto.CourseResultResponse;
import com.fap.result.dto.UpdateCourseResultRequest;
import com.fap.result.entity.CourseResult;
import com.fap.result.entity.CourseResultAdjustment;
import com.fap.result.entity.CourseResultQuiz;
import com.fap.result.enums.CourseResultStatus;
import com.fap.result.mapper.CourseResultMapper;
import com.fap.result.repository.ClassCompletionQuizRepository;
import com.fap.result.repository.CourseResultAdjustmentRepository;
import com.fap.result.repository.CourseResultQuizRepository;
import com.fap.result.repository.CourseResultRepository;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.repository.TrainingSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseResultService {
	private final ClassRepository classRepository;
	private final ClassCompletionQuizRepository completionQuizRepository;
	private final CourseResultRepository courseResultRepository;
	private final CourseResultQuizRepository resultQuizRepository;
	private final CourseResultAdjustmentRepository adjustmentRepository;
	private final TrainingSessionRepository trainingSessionRepository;
	private final CourseResultCalculator courseResultCalculator;
	private final CourseResultMapper courseResultMapper;
	private final AuditLogService auditLogService;
	private final NotificationService notificationService;
	private final MessageService messageService;

	public CourseResultService(
			ClassRepository classRepository,
			ClassCompletionQuizRepository completionQuizRepository,
			CourseResultRepository courseResultRepository,
			CourseResultQuizRepository resultQuizRepository,
			CourseResultAdjustmentRepository adjustmentRepository,
			TrainingSessionRepository trainingSessionRepository,
			CourseResultCalculator courseResultCalculator,
			CourseResultMapper courseResultMapper,
			AuditLogService auditLogService,
			NotificationService notificationService,
			MessageService messageService) {
		this.classRepository = classRepository;
		this.completionQuizRepository = completionQuizRepository;
		this.courseResultRepository = courseResultRepository;
		this.resultQuizRepository = resultQuizRepository;
		this.adjustmentRepository = adjustmentRepository;
		this.trainingSessionRepository = trainingSessionRepository;
		this.courseResultCalculator = courseResultCalculator;
		this.courseResultMapper = courseResultMapper;
		this.auditLogService = auditLogService;
		this.notificationService = notificationService;
		this.messageService = messageService;
	}

	@Transactional(readOnly = true)
	public ClassCourseResultsResponse list(Long classId) {
		FapClass fapClass = classRepository.getWithTrainingProgramOrThrow(classId);
		// Children of every result in two class-wide queries instead of two queries per result.
		Map<Long, List<CourseResultQuiz>> quizzesByResult = resultQuizRepository
				.findByCourseResultFapClassIdOrderByIdAsc(classId).stream()
				.collect(Collectors.groupingBy(item -> item.getCourseResult().getId()));
		Map<Long, List<CourseResultAdjustment>> adjustmentsByResult = adjustmentRepository
				.findByCourseResultFapClassIdOrderByAdjustedAtDescIdDesc(classId).stream()
				.collect(Collectors.groupingBy(item -> item.getCourseResult().getId()));
		List<CourseResultResponse> results = courseResultRepository
				.findByFapClassIdOrderByClassEnrollmentUserFullNameAsc(classId).stream()
				.map(result -> courseResultMapper.toResponse(
						result,
						quizzesByResult.getOrDefault(result.getId(), List.of()),
						adjustmentsByResult.getOrDefault(result.getId(), List.of())))
				.toList();
		return courseResultMapper.toClassResultsResponse(classId, fapClass, results);
	}

	@Transactional(readOnly = true)
	public CourseResultResponse get(Long classId, Long userId) {
		return toResponse(courseResultRepository.getByFapClassIdAndUserIdOrThrow(classId, userId));
	}

	@Transactional(readOnly = true)
	public CourseResultResponse getMine(Long classId, Long userId) {
		CourseResult result = courseResultRepository.getByFapClassIdAndUserIdOrThrow(classId, userId);
		if (result.getPublishedAt() == null) {
			throw new ConflictException("COURSE_RESULT_NOT_PUBLISHED", "Course result has not been published");
		}
		return toResponse(result);
	}

	@Transactional
	public ClassCourseResultsResponse calculate(Long classId, Long currentUserId) {
		FapClass fapClass = classRepository.getWithTrainingProgramForUpdateOrThrow(classId);
		if (fapClass.getStatus() != ClassStatus.Active) {
			throw new ConflictException("CLASS_RESULT_NOT_CALCULABLE", "Only active class results can be calculated");
		}
		courseResultCalculator.calculateAll(fapClass, currentUserId);
		auditLogService.record("CALCULATE_COURSE_RESULTS", "class", classId);
		return list(classId);
	}

	@Transactional
	public void finalizeForClosure(FapClass fapClass, Long currentUserId) {
		validateSessionsForClosure(fapClass.getId());
		validateRequiredQuizzesForClosure(fapClass.getId());
		List<CourseResult> results = courseResultCalculator.calculateAll(fapClass, currentUserId);
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
		CourseResult result = courseResultRepository.getForUpdateOrThrow(classId, userId);
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
		FapClass fapClass = classRepository.getWithTrainingProgramForUpdateOrThrow(classId);
		if (fapClass.getStatus() != ClassStatus.Closed) {
			throw new ConflictException("CLASS_NOT_CLOSED", "Course results can only be published after the class is closed");
		}
		List<CourseResult> results = courseResultRepository.findByFapClassIdOrderByClassEnrollmentUserFullNameAsc(classId);
		if (results.isEmpty() || results.stream().anyMatch(result -> result.effectiveStatus() == CourseResultStatus.InProgress)) {
			throw new ConflictException("COURSE_RESULTS_INCOMPLETE", "All course results must be calculated before publication")
					.withMessageKey("error.COURSE_RESULTS_INCOMPLETE.publication");
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

	// Kept out of the mapper because it queries; list() reads the same children class-wide instead.
	private CourseResultResponse toResponse(CourseResult result) {
		return courseResultMapper.toResponse(
				result,
				resultQuizRepository.findByCourseResultIdOrderByIdAsc(result.getId()),
				adjustmentRepository.findByCourseResultIdOrderByAdjustedAtDescIdDesc(result.getId()));
	}
}
