package com.fap.result.service;

import com.fap.clazz.entity.ClassEnrollment;
import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassEnrollmentStatus;
import com.fap.clazz.repository.ClassEnrollmentRepository;
import com.fap.quiz.repository.QuizAttemptRepository;
import com.fap.result.entity.ClassCompletionQuiz;
import com.fap.result.entity.CourseResult;
import com.fap.result.entity.CourseResultQuiz;
import com.fap.result.enums.CourseResultStatus;
import com.fap.result.repository.ClassCompletionQuizRepository;
import com.fap.result.repository.CourseResultQuizRepository;
import com.fap.result.repository.CourseResultRepository;
import com.fap.training.entity.AttendanceRecord;
import com.fap.training.entity.TrainingRegistration;
import com.fap.training.enums.AttendanceStatus;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.repository.AttendanceRecordRepository;
import com.fap.training.repository.TrainingRegistrationRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CourseResultCalculator {
	private static final Collection<ClassEnrollmentStatus> RESULT_ENROLLMENT_STATUSES = List.of(
			ClassEnrollmentStatus.Enrolled,
			ClassEnrollmentStatus.Completed,
			ClassEnrollmentStatus.Withdrawn);
	private static final Collection<ClassEnrollmentStatus> RECALCULATED_ENROLLMENT_STATUSES = List.of(
			ClassEnrollmentStatus.Enrolled,
			ClassEnrollmentStatus.Completed);
	private static final Collection<TrainingRegistrationStatus> ATTENDANCE_REGISTRATION_STATUSES = List.of(
			TrainingRegistrationStatus.Registered,
			TrainingRegistrationStatus.Completed);

	private final ClassEnrollmentRepository classEnrollmentRepository;
	private final ClassCompletionQuizRepository completionQuizRepository;
	private final CourseResultRepository courseResultRepository;
	private final CourseResultQuizRepository resultQuizRepository;
	private final QuizAttemptRepository quizAttemptRepository;
	private final TrainingRegistrationRepository trainingRegistrationRepository;
	private final AttendanceRecordRepository attendanceRecordRepository;

	public CourseResultCalculator(
			ClassEnrollmentRepository classEnrollmentRepository,
			ClassCompletionQuizRepository completionQuizRepository,
			CourseResultRepository courseResultRepository,
			CourseResultQuizRepository resultQuizRepository,
			QuizAttemptRepository quizAttemptRepository,
			TrainingRegistrationRepository trainingRegistrationRepository,
			AttendanceRecordRepository attendanceRecordRepository) {
		this.classEnrollmentRepository = classEnrollmentRepository;
		this.completionQuizRepository = completionQuizRepository;
		this.courseResultRepository = courseResultRepository;
		this.resultQuizRepository = resultQuizRepository;
		this.quizAttemptRepository = quizAttemptRepository;
		this.trainingRegistrationRepository = trainingRegistrationRepository;
		this.attendanceRecordRepository = attendanceRecordRepository;
	}

	/**
	 * Recalculates every result of the class from class-wide reads: results, registrations,
	 * attendance and best attempts are each loaded once and grouped by user, instead of 4 queries
	 * plus one per required quiz for every enrollment.
	 *
	 * <p>Deliberately not {@code @Transactional}: it must run inside the caller's write
	 * transaction, which already holds the class row lock.
	 */
	public List<CourseResult> calculateAll(FapClass fapClass, Long currentUserId) {
		Long classId = fapClass.getId();
		List<ClassCompletionQuiz> requiredQuizzes = completionQuizRepository.findByFapClassIdOrderByIdAsc(classId);
		List<ClassEnrollment> enrollments = classEnrollmentRepository
				.findByFapClassIdAndStatusInOrderByCreatedAtAscIdAsc(classId, RESULT_ENROLLMENT_STATUSES);
		Map<Long, CourseResult> resultsByEnrollment = courseResultRepository.findByFapClassId(classId).stream()
				.collect(Collectors.toMap(result -> result.getClassEnrollment().getId(), Function.identity()));

		Map<Long, Set<Long>> completedSessionsByUser = new HashMap<>();
		for (TrainingRegistration registration : trainingRegistrationRepository
				.findByClassIdAndStatusIn(classId, ATTENDANCE_REGISTRATION_STATUSES)) {
			if (registration.getTrainingSession().getStatus() == TrainingSessionStatus.Completed) {
				completedSessionsByUser.computeIfAbsent(registration.getUser().getId(), id -> new HashSet<>())
						.add(registration.getTrainingSession().getId());
			}
		}
		Map<Long, Map<Long, AttendanceStatus>> attendanceByUser = new HashMap<>();
		for (AttendanceRecord attendance : attendanceRecordRepository.findByTrainingSessionFapClassId(classId)) {
			attendanceByUser.computeIfAbsent(attendance.getUser().getId(), id -> new HashMap<>())
					.put(attendance.getTrainingSession().getId(), attendance.getStatus());
		}
		Map<String, QuizAttemptRepository.ScoredAttempt> bestAttempts = new HashMap<>();
		if (!requiredQuizzes.isEmpty()) {
			List<Long> quizIds = requiredQuizzes.stream().map(item -> item.getQuiz().getId()).toList();
			// Rows arrive best first, so the first row per quiz and user is the best attempt.
			quizAttemptRepository.findSubmittedForClassOrderByScoreDesc(classId, quizIds)
					.forEach(attempt -> bestAttempts.putIfAbsent(
							attemptKey(attempt.getQuizId(), attempt.getUserId()), attempt));
		}

		List<CourseResult> results = new ArrayList<>(enrollments.size());
		for (ClassEnrollment enrollment : enrollments) {
			CourseResult result = resultsByEnrollment.get(enrollment.getId());
			if (result == null) {
				result = new CourseResult();
				result.setFapClass(fapClass);
				result.setClassEnrollment(enrollment);
				result.setUpdatedAt(LocalDateTime.now());
				result = courseResultRepository.save(result);
			}
			results.add(result);
		}

		// Snapshots of every non-withdrawn result are rebuilt below; withdrawn ones keep theirs.
		resultQuizRepository.deleteForClassEnrollmentStatuses(classId, RECALCULATED_ENROLLMENT_STATUSES);
		List<CourseResultQuiz> snapshots = new ArrayList<>();
		for (int index = 0; index < enrollments.size(); index++) {
			ClassEnrollment enrollment = enrollments.get(index);
			CourseResult result = results.get(index);
			Long userId = enrollment.getUser().getId();
			if (enrollment.getStatus() == ClassEnrollmentStatus.Withdrawn) {
				markCalculatedWithdrawn(result, currentUserId);
				continue;
			}
			calculateOne(
					fapClass,
					result,
					requiredQuizzes,
					completedSessionsByUser.getOrDefault(userId, Set.of()),
					attendanceByUser.getOrDefault(userId, Map.of()),
					quizId -> bestAttempts.get(attemptKey(quizId, userId)),
					snapshots,
					currentUserId);
		}
		resultQuizRepository.saveAll(snapshots);
		return results;
	}

	private static String attemptKey(Long quizId, Long userId) {
		return quizId + ":" + userId;
	}

	private static void markCalculatedWithdrawn(CourseResult result, Long currentUserId) {
		result.setCalculatedStatus(CourseResultStatus.Withdrawn);
		result.setOverrideStatus(null);
		result.setCalculatedAt(LocalDateTime.now());
		result.setCalculatedBy(currentUserId);
		result.setUpdatedAt(LocalDateTime.now());
	}

	private void calculateOne(
			FapClass fapClass,
			CourseResult result,
			List<ClassCompletionQuiz> requiredQuizzes,
			Set<Long> completedSessionIds,
			Map<Long, AttendanceStatus> attendanceBySession,
			Function<Long, QuizAttemptRepository.ScoredAttempt> bestAttemptForQuiz,
			List<CourseResultQuiz> snapshots,
			Long currentUserId) {
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

		int passedQuizCount = 0;
		for (ClassCompletionQuiz requiredQuiz : requiredQuizzes) {
			QuizAttemptRepository.ScoredAttempt attempt = bestAttemptForQuiz.apply(requiredQuiz.getQuiz().getId());
			boolean passed = attempt != null && attempt.getScore() != null
					&& attempt.getScore() >= requiredQuiz.getPassingScore();
			CourseResultQuiz snapshot = new CourseResultQuiz();
			snapshot.setCourseResult(result);
			snapshot.setQuiz(requiredQuiz.getQuiz());
			snapshot.setRequiredScore(requiredQuiz.getPassingScore());
			snapshot.setBestAttemptId(attempt == null ? null : attempt.getId());
			snapshot.setBestScore(attempt == null ? null : attempt.getScore());
			snapshot.setPassed(passed);
			snapshots.add(snapshot);
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
	}
}
