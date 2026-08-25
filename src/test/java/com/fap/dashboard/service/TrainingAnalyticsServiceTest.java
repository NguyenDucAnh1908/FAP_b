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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingAnalyticsServiceTest {

	@Mock
	private TrainingSessionRepository trainingSessionRepository;

	@Mock
	private TrainingRegistrationRepository trainingRegistrationRepository;

	@Mock
	private AttendanceRecordRepository attendanceRecordRepository;

	@Mock
	private TrainingFeedbackRepository trainingFeedbackRepository;

	@Mock
	private ClassEnrollmentRepository classEnrollmentRepository;

	private TrainingAnalyticsService service;

	@BeforeEach
	void setUp() {
		service = new TrainingAnalyticsService(
				trainingSessionRepository,
				trainingRegistrationRepository,
				attendanceRecordRepository,
				trainingFeedbackRepository,
				classEnrollmentRepository);
	}

	@Test
	void calculatesSystemTrainingRatesAndBreakdowns() {
		List<TrainingSessionRepository.AnalyticsSessionStatusCount> sessionCounts = List.of(
				sessionCount(TrainingSessionStatus.Upcoming, 4),
				sessionCount(TrainingSessionStatus.Completed, 3),
				sessionCount(TrainingSessionStatus.Canceled, 1));
		List<AttendanceRecordRepository.AnalyticsAttendanceStatusCount> attendanceCounts = List.of(
				attendanceCount(AttendanceStatus.Present, 8),
				attendanceCount(AttendanceStatus.Late, 1),
				attendanceCount(AttendanceStatus.Absent, 1));
		List<TrainingRegistrationRepository.AnalyticsRegistrationStatusCount> registrationCounts = List.of(
				registrationCount(TrainingRegistrationStatus.Registered, 6),
				registrationCount(TrainingRegistrationStatus.Waitlist, 2));
		TrainingFeedbackRepository.AnalyticsFeedbackSummary feedback =
				feedbackSummary(3, 4.0, 5.0, 3.0);

		when(trainingSessionRepository.countStatusesForAnalytics(null, null, null)).thenReturn(sessionCounts);
		when(attendanceRecordRepository.countStatusesForAnalytics(null, null, null)).thenReturn(attendanceCounts);
		when(trainingRegistrationRepository.countStatusesForAnalytics(null, null, null)).thenReturn(registrationCounts);
		when(classEnrollmentRepository.countDistinctParticipantsForAnalytics(
				isNull(), anyList(), isNull(), isNull())).thenReturn(6L);
		when(trainingSessionRepository.countPendingAttendanceForAnalytics(
				isNull(), any(), any(), any(), isNull(), isNull())).thenReturn(2L);
		when(trainingFeedbackRepository.summarizeForAnalytics(null, null, null)).thenReturn(feedback);

		TrainingAnalyticsResponse response = service.getAnalytics(null, null, null);

		assertThat(response.scope()).isEqualTo("System");
		assertThat(response.totalSessions()).isEqualTo(8);
		assertThat(response.totalParticipants()).isEqualTo(6);
		assertThat(response.pendingAttendanceSessions()).isEqualTo(2);
		assertThat(response.totalAttendanceRecords()).isEqualTo(10);
		assertThat(response.attendanceRate()).isEqualTo(90.0);
		assertThat(response.completionRate()).isEqualTo(42.9);
		assertThat(response.feedbackResponses()).isEqualTo(3);
		assertThat(response.averageFeedbackRating()).isEqualTo(4.0);
		assertThat(statusCount(response.sessionStatuses(), "Canceled")).isEqualTo(1);
		assertThat(statusCount(response.registrationStatuses(), "Completed")).isZero();
	}

	@Test
	void scopesAnalyticsToCurrentClassAdmin() {
		Long classAdminId = 77L;
		LocalDate fromDate = LocalDate.of(2026, 1, 1);
		LocalDate toDate = LocalDate.of(2026, 12, 31);
		TrainingFeedbackRepository.AnalyticsFeedbackSummary feedback =
				feedbackSummary(0, null, null, null);
		when(trainingSessionRepository.countStatusesForAnalytics(classAdminId, fromDate, toDate))
				.thenReturn(List.of());
		when(attendanceRecordRepository.countStatusesForAnalytics(classAdminId, fromDate, toDate))
				.thenReturn(List.of());
		when(trainingRegistrationRepository.countStatusesForAnalytics(classAdminId, fromDate, toDate))
				.thenReturn(List.of());
		when(classEnrollmentRepository.countDistinctParticipantsForAnalytics(
				classAdminId, List.of(ClassEnrollmentStatus.Enrolled, ClassEnrollmentStatus.Completed),
				fromDate, toDate)).thenReturn(0L);
		when(trainingSessionRepository.countPendingAttendanceForAnalytics(
				classAdminId,
				TrainingSessionStatus.Upcoming,
				TrainingRegistrationStatus.Registered,
				LocalDate.now(),
				fromDate,
				toDate)).thenReturn(0L);
		when(trainingFeedbackRepository.summarizeForAnalytics(classAdminId, fromDate, toDate))
				.thenReturn(feedback);

		TrainingAnalyticsResponse response = service.getAnalytics(classAdminId, fromDate, toDate);

		assertThat(response.scope()).isEqualTo("AssignedClasses");
		assertThat(response.fromDate()).isEqualTo(fromDate);
		assertThat(response.toDate()).isEqualTo(toDate);
		assertThat(response.attendanceRate()).isZero();
		assertThat(response.completionRate()).isZero();
		assertThat(response.averageFeedbackRating()).isZero();
	}

	@Test
	void rejectsAnInvertedDateRangeBeforeQueryingRepositories() {
		LocalDate fromDate = LocalDate.of(2026, 8, 2);
		LocalDate toDate = LocalDate.of(2026, 8, 1);

		assertThatThrownBy(() -> service.getAnalytics(null, fromDate, toDate))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("From date must not be after to date");
		verify(trainingSessionRepository, never()).countStatusesForAnalytics(any(), any(), any());
	}

	private long statusCount(List<TrainingAnalyticsResponse.StatusCount> items, String status) {
		return items.stream()
				.filter(item -> item.status().equals(status))
				.findFirst()
				.orElseThrow()
				.count();
	}

	private TrainingSessionRepository.AnalyticsSessionStatusCount sessionCount(
			TrainingSessionStatus status, long total) {
		TrainingSessionRepository.AnalyticsSessionStatusCount item =
				mock(TrainingSessionRepository.AnalyticsSessionStatusCount.class);
		when(item.getStatus()).thenReturn(status);
		when(item.getTotal()).thenReturn(total);
		return item;
	}

	private AttendanceRecordRepository.AnalyticsAttendanceStatusCount attendanceCount(
			AttendanceStatus status, long total) {
		AttendanceRecordRepository.AnalyticsAttendanceStatusCount item =
				mock(AttendanceRecordRepository.AnalyticsAttendanceStatusCount.class);
		when(item.getStatus()).thenReturn(status);
		when(item.getTotal()).thenReturn(total);
		return item;
	}

	private TrainingRegistrationRepository.AnalyticsRegistrationStatusCount registrationCount(
			TrainingRegistrationStatus status, long total) {
		TrainingRegistrationRepository.AnalyticsRegistrationStatusCount item =
				mock(TrainingRegistrationRepository.AnalyticsRegistrationStatusCount.class);
		when(item.getStatus()).thenReturn(status);
		when(item.getTotal()).thenReturn(total);
		return item;
	}

	private TrainingFeedbackRepository.AnalyticsFeedbackSummary feedbackSummary(
			long total, Double content, Double trainer, Double organization) {
		TrainingFeedbackRepository.AnalyticsFeedbackSummary summary =
				mock(TrainingFeedbackRepository.AnalyticsFeedbackSummary.class);
		when(summary.getFeedbackCount()).thenReturn(total);
		if (total > 0) {
			when(summary.getAverageContentRating()).thenReturn(content);
			when(summary.getAverageTrainerRating()).thenReturn(trainer);
			when(summary.getAverageOrganizationRating()).thenReturn(organization);
		}
		return summary;
	}
}
