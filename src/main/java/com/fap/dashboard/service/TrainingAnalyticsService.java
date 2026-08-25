package com.fap.dashboard.service;

import com.fap.common.exception.BadRequestException;
import com.fap.clazz.enums.ClassEnrollmentStatus;
import com.fap.clazz.repository.ClassEnrollmentRepository;
import com.fap.dashboard.dto.TrainingAnalyticsResponse;
import com.fap.training.enums.AttendanceStatus;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.repository.AttendanceRecordRepository;
import com.fap.training.repository.TrainingFeedbackRepository;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fap.training.repository.TrainingSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class TrainingAnalyticsService {

	private final TrainingSessionRepository trainingSessionRepository;
	private final TrainingRegistrationRepository trainingRegistrationRepository;
	private final AttendanceRecordRepository attendanceRecordRepository;
	private final TrainingFeedbackRepository trainingFeedbackRepository;
	private final ClassEnrollmentRepository classEnrollmentRepository;

	public TrainingAnalyticsService(
			TrainingSessionRepository trainingSessionRepository,
			TrainingRegistrationRepository trainingRegistrationRepository,
			AttendanceRecordRepository attendanceRecordRepository,
			TrainingFeedbackRepository trainingFeedbackRepository,
			ClassEnrollmentRepository classEnrollmentRepository) {
		this.trainingSessionRepository = trainingSessionRepository;
		this.trainingRegistrationRepository = trainingRegistrationRepository;
		this.attendanceRecordRepository = attendanceRecordRepository;
		this.trainingFeedbackRepository = trainingFeedbackRepository;
		this.classEnrollmentRepository = classEnrollmentRepository;
	}

	@Transactional(readOnly = true)
	public TrainingAnalyticsResponse getAnalytics(Long classAdminId, LocalDate fromDate, LocalDate toDate) {
		validateDateRange(fromDate, toDate);

		Map<TrainingSessionStatus, Long> sessions = sessionCounts(classAdminId, fromDate, toDate);
		Map<AttendanceStatus, Long> attendance = attendanceCounts(classAdminId, fromDate, toDate);
		Map<TrainingRegistrationStatus, Long> registrations = registrationCounts(classAdminId, fromDate, toDate);

		long totalSessions = sessions.values().stream().mapToLong(Long::longValue).sum();
		long completedSessions = sessions.getOrDefault(TrainingSessionStatus.Completed, 0L);
		long eligibleSessions = totalSessions - sessions.getOrDefault(TrainingSessionStatus.Canceled, 0L);
		long totalAttendance = attendance.values().stream().mapToLong(Long::longValue).sum();
		long attended = attendance.getOrDefault(AttendanceStatus.Present, 0L)
				+ attendance.getOrDefault(AttendanceStatus.Late, 0L);

		TrainingFeedbackRepository.AnalyticsFeedbackSummary feedback =
				trainingFeedbackRepository.summarizeForAnalytics(classAdminId, fromDate, toDate);
		long feedbackResponses = valueOrZero(feedback == null ? null : feedback.getFeedbackCount());
		double averageFeedbackRating = averageFeedbackRating(feedback);

		return new TrainingAnalyticsResponse(
				classAdminId == null ? "System" : "AssignedClasses",
				fromDate,
				toDate,
				totalSessions,
				classEnrollmentRepository.countDistinctParticipantsForAnalytics(
						classAdminId,
						List.of(ClassEnrollmentStatus.Enrolled, ClassEnrollmentStatus.Completed),
						fromDate,
						toDate),
				trainingSessionRepository.countPendingAttendanceForAnalytics(
						classAdminId,
						TrainingSessionStatus.Upcoming,
						TrainingRegistrationStatus.Registered,
						LocalDate.now(),
						fromDate,
						toDate),
				totalAttendance,
				percentage(attended, totalAttendance),
				percentage(completedSessions, eligibleSessions),
				feedbackResponses,
				averageFeedbackRating,
				statusCounts(TrainingSessionStatus.values(), sessions),
				statusCounts(AttendanceStatus.values(), attendance),
				statusCounts(TrainingRegistrationStatus.values(), registrations),
				LocalDateTime.now());
	}

	private Map<TrainingSessionStatus, Long> sessionCounts(
			Long classAdminId, LocalDate fromDate, LocalDate toDate) {
		Map<TrainingSessionStatus, Long> counts = new EnumMap<>(TrainingSessionStatus.class);
		trainingSessionRepository.countStatusesForAnalytics(classAdminId, fromDate, toDate)
				.forEach(item -> counts.put(item.getStatus(), valueOrZero(item.getTotal())));
		return counts;
	}

	private Map<AttendanceStatus, Long> attendanceCounts(
			Long classAdminId, LocalDate fromDate, LocalDate toDate) {
		Map<AttendanceStatus, Long> counts = new EnumMap<>(AttendanceStatus.class);
		attendanceRecordRepository.countStatusesForAnalytics(classAdminId, fromDate, toDate)
				.forEach(item -> counts.put(item.getStatus(), valueOrZero(item.getTotal())));
		return counts;
	}

	private Map<TrainingRegistrationStatus, Long> registrationCounts(
			Long classAdminId, LocalDate fromDate, LocalDate toDate) {
		Map<TrainingRegistrationStatus, Long> counts = new EnumMap<>(TrainingRegistrationStatus.class);
		trainingRegistrationRepository.countStatusesForAnalytics(classAdminId, fromDate, toDate)
				.forEach(item -> counts.put(item.getStatus(), valueOrZero(item.getTotal())));
		return counts;
	}

	private <E extends Enum<E>> List<TrainingAnalyticsResponse.StatusCount> statusCounts(
			E[] statuses, Map<E, Long> counts) {
		return Arrays.stream(statuses)
				.map(status -> new TrainingAnalyticsResponse.StatusCount(
						status.name(), counts.getOrDefault(status, 0L)))
				.toList();
	}

	private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
		if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
			throw new BadRequestException("INVALID_DATE_RANGE", "From date must not be after to date");
		}
	}

	private double averageFeedbackRating(TrainingFeedbackRepository.AnalyticsFeedbackSummary feedback) {
		if (feedback == null || valueOrZero(feedback.getFeedbackCount()) == 0) {
			return 0;
		}
		double content = valueOrZero(feedback.getAverageContentRating());
		double trainer = valueOrZero(feedback.getAverageTrainerRating());
		double organization = valueOrZero(feedback.getAverageOrganizationRating());
		return roundOneDecimal((content + trainer + organization) / 3);
	}

	private double percentage(long numerator, long denominator) {
		return denominator == 0 ? 0 : roundOneDecimal(numerator * 100.0 / denominator);
	}

	private double roundOneDecimal(double value) {
		return Math.round(value * 10.0) / 10.0;
	}

	private long valueOrZero(Long value) {
		return value == null ? 0 : value;
	}

	private double valueOrZero(Double value) {
		return value == null ? 0 : value;
	}
}
