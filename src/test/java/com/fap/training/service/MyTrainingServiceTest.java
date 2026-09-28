package com.fap.training.service;

import com.fap.clazz.dto.ClassResponse;
import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.mapper.ClassMapper;
import com.fap.clazz.repository.ClassRepository;
import com.fap.clazz.repository.ClassTrainerRepository;
import com.fap.common.exception.BadRequestException;
import com.fap.training.dto.MyAttendanceResponse;
import com.fap.training.dto.MyClassAdminDashboardResponse;
import com.fap.training.dto.MyTrainerDashboardResponse;
import com.fap.training.dto.MyTrainingDashboardResponse;
import com.fap.training.dto.MyTrainingRegistrationResponse;
import com.fap.training.dto.MyTrainingSessionResponse;
import com.fap.training.dto.TrainingSessionResponse;
import com.fap.training.entity.AttendanceRecord;
import com.fap.training.entity.TrainingRegistration;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.AttendanceStatus;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.mapper.MyTrainingMapper;
import com.fap.training.mapper.TrainingSessionMapper;
import com.fap.training.repository.AttendanceRecordRepository;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fap.training.repository.TrainingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The personal "my training" lists and the three role dashboards. The lists validate the client's
 * date range and sort field before touching the database, each against its own whitelist; the
 * dashboards are fixed aggregates whose "today", status filters and top-5 page shapes are the whole
 * contract, so a wrong status or date here silently shows the wrong numbers rather than failing.
 */
class MyTrainingServiceTest {

	private static final long USER_ID = 7L;
	private static final LocalDate JUNE_1 = LocalDate.of(2026, 6, 1);
	private static final LocalDate JUNE_2 = LocalDate.of(2026, 6, 2);
	private static final Sort SESSIONS_EARLIEST_FIRST = Sort.by(Sort.Direction.ASC, "sessionDate", "startTime", "id");
	private static final Sort SESSIONS_LATEST_FIRST = Sort.by(Sort.Direction.DESC, "sessionDate", "startTime", "id");

	private final TrainingRegistrationRepository trainingRegistrationRepository =
			mock(TrainingRegistrationRepository.class);
	private final AttendanceRecordRepository attendanceRecordRepository = mock(AttendanceRecordRepository.class);
	private final TrainingSessionRepository trainingSessionRepository = mock(TrainingSessionRepository.class);
	private final ClassRepository classRepository = mock(ClassRepository.class);
	private final ClassTrainerRepository classTrainerRepository = mock(ClassTrainerRepository.class);
	private final ClassMapper classMapper = mock(ClassMapper.class);
	private final MyTrainingMapper myTrainingMapper = mock(MyTrainingMapper.class);
	private final TrainingSessionMapper trainingSessionMapper = mock(TrainingSessionMapper.class);

	private final MyTrainingService service = new MyTrainingService(
			trainingRegistrationRepository,
			attendanceRecordRepository,
			trainingSessionRepository,
			classRepository,
			classTrainerRepository,
			classMapper,
			myTrainingMapper,
			trainingSessionMapper);

	private LocalDate todayBeforeCall;

	@BeforeEach
	void returnEmptyPages() {
		when(trainingRegistrationRepository.searchMine(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(Page.empty());
		when(attendanceRecordRepository.searchMine(any(), any(), any(), any(), any(), any()))
				.thenReturn(Page.empty());
		when(trainingSessionRepository.search(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(Page.empty());
		when(trainingSessionRepository.searchByClassAdminId(any(), any(), any(), any(), any()))
				.thenReturn(Page.empty());
		when(classRepository.searchByAdminId(any(), any(), any(), any(), any()))
				.thenReturn(Page.empty());
	}

	@BeforeEach
	void rememberToday() {
		todayBeforeCall = LocalDate.now();
	}

	@ParameterizedTest(name = "{0} rejects a from date after the to date")
	@MethodSource("dateFilteredQueries")
	void rejectsInvertedDateRangeBeforeQuerying(DateFilteredQuery query) {
		assertThatThrownBy(() -> query.run(service, JUNE_2, JUNE_1))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_MY_TRAINING_DATE_FILTER");

		verifyNoInteractions(trainingRegistrationRepository, attendanceRecordRepository);
	}

	@ParameterizedTest(name = "{0} accepts a single-day range")
	@MethodSource("dateFilteredQueries")
	void acceptsSingleDayRange(DateFilteredQuery query) {
		assertThatCode(() -> query.run(service, JUNE_1, JUNE_1)).doesNotThrowAnyException();
	}

	@ParameterizedTest(name = "{0} accepts a range open at either end")
	@MethodSource("dateFilteredQueries")
	void acceptsOpenEndedRange(DateFilteredQuery query) {
		assertThatCode(() -> query.run(service, JUNE_2, null)).doesNotThrowAnyException();
		assertThatCode(() -> query.run(service, null, JUNE_1)).doesNotThrowAnyException();
	}

	@Test
	void registrationsTrimKeywordAndSortNewestRegistrationFirst() {
		TrainingRegistration registration = new TrainingRegistration();
		MyTrainingRegistrationResponse mapped = mock(MyTrainingRegistrationResponse.class);
		when(trainingRegistrationRepository.searchMine(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(registration)));
		when(myTrainingMapper.toRegistrationResponse(registration)).thenReturn(mapped);

		Page<MyTrainingRegistrationResponse> page = service.registrations(
				USER_ID,
				TrainingRegistrationStatus.Registered,
				TrainingSessionStatus.Upcoming,
				JUNE_1,
				JUNE_2,
				" java ",
				1,
				20);

		assertThat(page.getContent()).containsExactly(mapped);
		verify(trainingRegistrationRepository).searchMine(
				USER_ID,
				TrainingRegistrationStatus.Registered,
				TrainingSessionStatus.Upcoming,
				JUNE_1,
				JUNE_2,
				"java",
				PageRequest.of(1, 20, Sort.by(Sort.Direction.DESC, "registeredAt")));
	}

	@Test
	void sessionsTreatBlankKeywordAsNoFilterAndSortOldestRegistrationFirst() {
		TrainingRegistration registration = new TrainingRegistration();
		MyTrainingSessionResponse mapped = mock(MyTrainingSessionResponse.class);
		when(trainingRegistrationRepository.searchMine(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(registration)));
		when(myTrainingMapper.toSessionResponse(registration)).thenReturn(mapped);

		Page<MyTrainingSessionResponse> page = service.sessions(
				USER_ID,
				null,
				TrainingSessionStatus.Completed,
				null,
				null,
				"   ",
				0,
				10);

		assertThat(page.getContent()).containsExactly(mapped);
		verify(trainingRegistrationRepository).searchMine(
				USER_ID,
				null,
				TrainingSessionStatus.Completed,
				null,
				null,
				null,
				PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "registeredAt")));
	}

	@Test
	void attendanceTrimsKeywordAndSortsNewestRecordFirst() {
		AttendanceRecord record = new AttendanceRecord();
		MyAttendanceResponse mapped = mock(MyAttendanceResponse.class);
		when(attendanceRecordRepository.searchMine(any(), any(), any(), any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(record)));
		when(myTrainingMapper.toAttendanceResponse(record)).thenReturn(mapped);

		Page<MyAttendanceResponse> page = service.attendance(
				USER_ID,
				AttendanceStatus.Late,
				JUNE_1,
				JUNE_2,
				" room 3 ",
				0,
				10);

		assertThat(page.getContent()).containsExactly(mapped);
		verify(attendanceRecordRepository).searchMine(
				USER_ID,
				AttendanceStatus.Late,
				JUNE_1,
				JUNE_2,
				"room 3",
				PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
	}

	@Test
	void attendanceAppliesRequestedWhitelistedSort() {
		service.attendance(USER_ID, null, null, null, null, 0, 10, "checkedInAt", "desc");

		verify(attendanceRecordRepository).searchMine(
				USER_ID,
				null,
				null,
				null,
				null,
				PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "checkedInAt")));
	}

	/** Each list has its own whitelist: a field that is valid for one list is rejected by another. */
	@ParameterizedTest(name = "{0} rejects sort field {1}")
	@MethodSource("sortFieldsOutsideWhitelist")
	void rejectsSortFieldOutsideItsWhitelist(SortedQuery query, String sortBy) {
		assertThatThrownBy(() -> query.run(service, sortBy))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_SORT_FIELD");

		verifyNoInteractions(trainingRegistrationRepository, attendanceRecordRepository);
	}

	@Test
	void dashboardCountsRegistrationsByStatus() {
		when(trainingRegistrationRepository.countMine(USER_ID, TrainingRegistrationStatus.Registered, null, null, null))
				.thenReturn(4L);
		when(trainingRegistrationRepository.countMine(
				eq(USER_ID),
				eq(TrainingRegistrationStatus.Registered),
				eq(TrainingSessionStatus.Upcoming),
				any(LocalDate.class),
				isNull()))
				.thenReturn(2L);
		when(trainingRegistrationRepository.countMine(USER_ID, TrainingRegistrationStatus.Completed, null, null, null))
				.thenReturn(3L);
		when(trainingRegistrationRepository.countMine(USER_ID, TrainingRegistrationStatus.Waitlist, null, null, null))
				.thenReturn(1L);

		MyTrainingDashboardResponse dashboard = service.dashboard(USER_ID);

		assertThat(dashboard.registeredSessions()).isEqualTo(4);
		assertThat(dashboard.upcomingSessions()).isEqualTo(2);
		assertThat(dashboard.completedSessions()).isEqualTo(3);
		assertThat(dashboard.waitlistedSessions()).isEqualTo(1);
		ArgumentCaptor<LocalDate> upcomingFrom = ArgumentCaptor.forClass(LocalDate.class);
		verify(trainingRegistrationRepository).countMine(
				eq(USER_ID),
				eq(TrainingRegistrationStatus.Registered),
				eq(TrainingSessionStatus.Upcoming),
				upcomingFrom.capture(),
				isNull());
		assertIsToday(upcomingFrom.getValue());
	}

	@Test
	void dashboardSummarizesAttendanceOverAllSessions() {
		when(attendanceRecordRepository.countMine(USER_ID, AttendanceStatus.Present, null, null)).thenReturn(5L);
		when(attendanceRecordRepository.countMine(USER_ID, AttendanceStatus.Late, null, null)).thenReturn(1L);
		when(attendanceRecordRepository.countMine(USER_ID, AttendanceStatus.Absent, null, null)).thenReturn(2L);

		MyTrainingDashboardResponse dashboard = service.dashboard(USER_ID);

		assertThat(dashboard.attendanceSummary())
				.isEqualTo(new MyTrainingDashboardResponse.AttendanceSummary(5, 1, 2));
	}

	@Test
	void dashboardListsNextFiveUpcomingSessionsAndFiveMostRecentAttendanceRecords() {
		TrainingRegistration registration = new TrainingRegistration();
		AttendanceRecord record = new AttendanceRecord();
		MyTrainingSessionResponse nextSession = mock(MyTrainingSessionResponse.class);
		MyAttendanceResponse recentRecord = mock(MyAttendanceResponse.class);
		when(trainingRegistrationRepository.searchMine(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(registration)));
		when(attendanceRecordRepository.searchMine(any(), any(), any(), any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(record)));
		when(myTrainingMapper.toSessionResponse(registration)).thenReturn(nextSession);
		when(myTrainingMapper.toAttendanceResponse(record)).thenReturn(recentRecord);

		MyTrainingDashboardResponse dashboard = service.dashboard(USER_ID);

		assertThat(dashboard.nextSessions()).containsExactly(nextSession);
		assertThat(dashboard.recentAttendance()).containsExactly(recentRecord);
		ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
		verify(trainingRegistrationRepository).searchMine(
				eq(USER_ID),
				eq(TrainingRegistrationStatus.Registered),
				eq(TrainingSessionStatus.Upcoming),
				from.capture(),
				isNull(),
				isNull(),
				eq(PageRequest.of(0, 5)));
		assertIsToday(from.getValue());
		verify(attendanceRecordRepository).searchMine(USER_ID, null, null, null, null, PageRequest.of(0, 5));
	}

	@Test
	void trainerDashboardAggregatesClassAndSessionCounts() {
		when(classTrainerRepository.countDistinctClassesByTrainerId(USER_ID)).thenReturn(3L);
		when(trainingSessionRepository.countByTrainerIdAndStatusAndSessionDateGreaterThanEqual(
				eq(USER_ID), eq(TrainingSessionStatus.Upcoming), any(LocalDate.class)))
				.thenReturn(6L);
		when(trainingSessionRepository.countByTrainerIdAndStatus(USER_ID, TrainingSessionStatus.Completed))
				.thenReturn(11L);
		when(trainingSessionRepository.countPendingAttendanceSessions(
				eq(USER_ID), eq(TrainingSessionStatus.Upcoming), eq(TrainingRegistrationStatus.Registered), any(LocalDate.class)))
				.thenReturn(2L);

		MyTrainerDashboardResponse dashboard = service.trainerDashboard(USER_ID);

		assertThat(dashboard.assignedClasses()).isEqualTo(3);
		assertThat(dashboard.upcomingSessions()).isEqualTo(6);
		assertThat(dashboard.completedSessions()).isEqualTo(11);
		assertThat(dashboard.pendingAttendanceSessions()).isEqualTo(2);
		ArgumentCaptor<LocalDate> upcomingFrom = ArgumentCaptor.forClass(LocalDate.class);
		verify(trainingSessionRepository).countByTrainerIdAndStatusAndSessionDateGreaterThanEqual(
				eq(USER_ID), eq(TrainingSessionStatus.Upcoming), upcomingFrom.capture());
		assertIsToday(upcomingFrom.getValue());
		ArgumentCaptor<LocalDate> pendingAsOf = ArgumentCaptor.forClass(LocalDate.class);
		verify(trainingSessionRepository).countPendingAttendanceSessions(
				eq(USER_ID), eq(TrainingSessionStatus.Upcoming), eq(TrainingRegistrationStatus.Registered), pendingAsOf.capture());
		assertIsToday(pendingAsOf.getValue());
	}

	@Test
	void trainerDashboardListsNextSessionsEarliestFirstAndCompletedSessionsLatestFirst() {
		TrainingSession upcoming = new TrainingSession();
		TrainingSession completed = new TrainingSession();
		TrainingSessionResponse upcomingResponse = mock(TrainingSessionResponse.class);
		TrainingSessionResponse completedResponse = mock(TrainingSessionResponse.class);
		when(trainingSessionRepository.search(eq(TrainingSessionStatus.Upcoming), any(), any(), any(), any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(upcoming)));
		when(trainingSessionRepository.search(eq(TrainingSessionStatus.Completed), any(), any(), any(), any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(completed)));
		when(trainingSessionMapper.toResponse(upcoming)).thenReturn(upcomingResponse);
		when(trainingSessionMapper.toResponse(completed)).thenReturn(completedResponse);

		MyTrainerDashboardResponse dashboard = service.trainerDashboard(USER_ID);

		assertThat(dashboard.nextSessions()).containsExactly(upcomingResponse);
		assertThat(dashboard.recentCompletedSessions()).containsExactly(completedResponse);
		ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
		verify(trainingSessionRepository).search(
				eq(TrainingSessionStatus.Upcoming),
				isNull(),
				eq(USER_ID),
				from.capture(),
				isNull(),
				isNull(),
				eq(PageRequest.of(0, 5, SESSIONS_EARLIEST_FIRST)));
		assertIsToday(from.getValue());
		verify(trainingSessionRepository).search(
				TrainingSessionStatus.Completed,
				null,
				USER_ID,
				null,
				null,
				null,
				PageRequest.of(0, 5, SESSIONS_LATEST_FIRST));
	}

	@Test
	void classAdminDashboardCountsClassesByStatus() {
		when(classRepository.countByAdminIdAndStatus(USER_ID, null)).thenReturn(5L);
		when(classRepository.countByAdminIdAndStatus(USER_ID, ClassStatus.Active)).thenReturn(2L);
		when(classRepository.countByAdminIdAndStatus(USER_ID, ClassStatus.Planning)).thenReturn(1L);

		MyClassAdminDashboardResponse dashboard = service.classAdminDashboard(USER_ID);

		assertThat(dashboard.assignedClasses()).isEqualTo(5);
		assertThat(dashboard.activeClasses()).isEqualTo(2);
		assertThat(dashboard.planningClasses()).isEqualTo(1);
	}

	@Test
	void classAdminDashboardCountsSessionsTrainersAndParticipants() {
		when(trainingSessionRepository.countByClassAdminId(
				eq(USER_ID), eq(TrainingSessionStatus.Upcoming), any(LocalDate.class), isNull()))
				.thenReturn(8L);
		when(trainingSessionRepository.countPendingAttendanceSessionsByClassAdminId(
				eq(USER_ID), eq(TrainingSessionStatus.Upcoming), eq(TrainingRegistrationStatus.Registered), any(LocalDate.class)))
				.thenReturn(3L);
		when(classTrainerRepository.countDistinctTrainersByClassAdminId(USER_ID)).thenReturn(4L);
		when(trainingRegistrationRepository.countByClassAdminIdAndStatus(USER_ID, TrainingRegistrationStatus.Registered))
				.thenReturn(40L);

		MyClassAdminDashboardResponse dashboard = service.classAdminDashboard(USER_ID);

		assertThat(dashboard.upcomingSessions()).isEqualTo(8);
		assertThat(dashboard.pendingAttendanceSessions()).isEqualTo(3);
		assertThat(dashboard.totalTrainers()).isEqualTo(4);
		assertThat(dashboard.totalParticipants()).isEqualTo(40);
		ArgumentCaptor<LocalDate> upcomingFrom = ArgumentCaptor.forClass(LocalDate.class);
		verify(trainingSessionRepository).countByClassAdminId(
				eq(USER_ID), eq(TrainingSessionStatus.Upcoming), upcomingFrom.capture(), isNull());
		assertIsToday(upcomingFrom.getValue());
		ArgumentCaptor<LocalDate> pendingAsOf = ArgumentCaptor.forClass(LocalDate.class);
		verify(trainingSessionRepository).countPendingAttendanceSessionsByClassAdminId(
				eq(USER_ID), eq(TrainingSessionStatus.Upcoming), eq(TrainingRegistrationStatus.Registered), pendingAsOf.capture());
		assertIsToday(pendingAsOf.getValue());
	}

	@Test
	void classAdminDashboardListsFiveClassesStartingFromToday() {
		FapClass fapClass = new FapClass();
		ClassResponse mapped = mock(ClassResponse.class);
		when(classRepository.searchByAdminId(any(), any(), any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(fapClass)));
		when(classMapper.toResponse(fapClass)).thenReturn(mapped);

		MyClassAdminDashboardResponse dashboard = service.classAdminDashboard(USER_ID);

		assertThat(dashboard.classesStartingSoon()).containsExactly(mapped);
		ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
		verify(classRepository).searchByAdminId(
				eq(USER_ID),
				isNull(),
				from.capture(),
				isNull(),
				eq(PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "startDate", "id"))));
		assertIsToday(from.getValue());
	}

	/**
	 * Pins current behaviour: "recent" sessions carry no status or date bound, so with a descending
	 * sort the furthest-scheduled future sessions come first rather than the most recently held ones.
	 */
	@Test
	void classAdminDashboardRecentSessionsAreUnboundedByStatusOrDate() {
		TrainingSession session = new TrainingSession();
		TrainingSessionResponse mapped = mock(TrainingSessionResponse.class);
		when(trainingSessionRepository.searchByClassAdminId(any(), any(), any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(session)));
		when(trainingSessionMapper.toResponse(session)).thenReturn(mapped);

		MyClassAdminDashboardResponse dashboard = service.classAdminDashboard(USER_ID);

		assertThat(dashboard.recentSessions()).containsExactly(mapped);
		verify(trainingSessionRepository).searchByClassAdminId(
				USER_ID,
				null,
				null,
				null,
				PageRequest.of(0, 5, SESSIONS_LATEST_FIRST));
	}

	@FunctionalInterface
	interface DateFilteredQuery {
		void run(MyTrainingService service, LocalDate fromDate, LocalDate toDate);
	}

	@FunctionalInterface
	interface SortedQuery {
		void run(MyTrainingService service, String sortBy);
	}

	static Stream<Arguments> dateFilteredQueries() {
		return Stream.of(
				Arguments.of(Named.of("registrations", (DateFilteredQuery) (service, fromDate, toDate) ->
						service.registrations(USER_ID, null, null, fromDate, toDate, null, 0, 20))),
				Arguments.of(Named.of("sessions", (DateFilteredQuery) (service, fromDate, toDate) ->
						service.sessions(USER_ID, null, null, fromDate, toDate, null, 0, 20))),
				Arguments.of(Named.of("attendance", (DateFilteredQuery) (service, fromDate, toDate) ->
						service.attendance(USER_ID, null, fromDate, toDate, null, 0, 20))));
	}

	static Stream<Arguments> sortFieldsOutsideWhitelist() {
		SortedQuery registrations = (service, sortBy) ->
				service.registrations(USER_ID, null, null, null, null, null, 0, 20, sortBy, "asc");
		SortedQuery sessions = (service, sortBy) ->
				service.sessions(USER_ID, null, null, null, null, null, 0, 20, sortBy, "asc");
		SortedQuery attendance = (service, sortBy) ->
				service.attendance(USER_ID, null, null, null, null, 0, 20, sortBy, "asc");
		return Stream.of(
				Arguments.of(Named.of("registrations", registrations), "sessionDate"),
				Arguments.of(Named.of("sessions", sessions), "sessionDate"),
				Arguments.of(Named.of("attendance", attendance), "registeredAt"),
				Arguments.of(Named.of("registrations", registrations), "checkedInAt"));
	}

	private void assertIsToday(LocalDate date) {
		// Bracketing the call keeps the check stable when the test runs across midnight.
		assertThat(date).isBetween(todayBeforeCall, LocalDate.now());
	}
}
