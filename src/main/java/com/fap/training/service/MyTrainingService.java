package com.fap.training.service;

import com.fap.clazz.dto.ClassResponse;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.mapper.ClassMapper;
import com.fap.clazz.repository.ClassRepository;
import com.fap.clazz.repository.ClassTrainerRepository;
import com.fap.common.exception.BadRequestException;
import com.fap.common.api.PageRequestFactory;
import com.fap.training.dto.MyAttendanceResponse;
import com.fap.training.dto.MyClassAdminDashboardResponse;
import com.fap.training.dto.MyTrainerDashboardResponse;
import com.fap.training.dto.MyTrainingDashboardResponse;
import com.fap.training.dto.MyTrainingRegistrationResponse;
import com.fap.training.dto.MyTrainingSessionResponse;
import com.fap.training.dto.TrainingSessionResponse;
import com.fap.training.enums.AttendanceStatus;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.mapper.MyTrainingMapper;
import com.fap.training.mapper.TrainingSessionMapper;
import com.fap.training.repository.AttendanceRecordRepository;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fap.training.repository.TrainingSessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MyTrainingService {

	private final TrainingRegistrationRepository trainingRegistrationRepository;
	private final AttendanceRecordRepository attendanceRecordRepository;
	private final TrainingSessionRepository trainingSessionRepository;
	private final ClassRepository classRepository;
	private final ClassTrainerRepository classTrainerRepository;
	private final ClassMapper classMapper;
	private final MyTrainingMapper myTrainingMapper;
	private final TrainingSessionMapper trainingSessionMapper;

	public MyTrainingService(
			TrainingRegistrationRepository trainingRegistrationRepository,
			AttendanceRecordRepository attendanceRecordRepository,
			TrainingSessionRepository trainingSessionRepository,
			ClassRepository classRepository,
			ClassTrainerRepository classTrainerRepository,
			ClassMapper classMapper,
			MyTrainingMapper myTrainingMapper,
			TrainingSessionMapper trainingSessionMapper) {
		this.trainingRegistrationRepository = trainingRegistrationRepository;
		this.attendanceRecordRepository = attendanceRecordRepository;
		this.trainingSessionRepository = trainingSessionRepository;
		this.classRepository = classRepository;
		this.classTrainerRepository = classTrainerRepository;
		this.classMapper = classMapper;
		this.myTrainingMapper = myTrainingMapper;
		this.trainingSessionMapper = trainingSessionMapper;
	}

	@Transactional(readOnly = true)
	public Page<MyTrainingRegistrationResponse> registrations(
			Long currentUserId,
			TrainingRegistrationStatus registrationStatus,
			TrainingSessionStatus sessionStatus,
			LocalDate fromDate,
			LocalDate toDate,
			String keyword,
			int page,
			int limit) {
		return registrations(currentUserId, registrationStatus, sessionStatus, fromDate, toDate, keyword, page, limit, null, null);
	}

	@Transactional(readOnly = true)
	public Page<MyTrainingRegistrationResponse> registrations(
			Long currentUserId,
			TrainingRegistrationStatus registrationStatus,
			TrainingSessionStatus sessionStatus,
			LocalDate fromDate,
			LocalDate toDate,
			String keyword,
			int page,
			int limit,
			String sortBy,
			String order) {
		validateDateFilter(fromDate, toDate);
		PageRequest pageRequest = PageRequestFactory.create(
				page,
				limit,
				sortBy,
				order,
				Sort.by(Sort.Direction.DESC, "registeredAt"),
				"id", "registeredAt", "cancelledAt", "completedAt", "status");
		return trainingRegistrationRepository
				.searchMine(
						currentUserId,
						registrationStatus,
						sessionStatus,
						fromDate,
						toDate,
						normalize(keyword),
						pageRequest)
				.map(myTrainingMapper::toRegistrationResponse);
	}

	@Transactional(readOnly = true)
	public Page<MyTrainingSessionResponse> sessions(
			Long currentUserId,
			TrainingRegistrationStatus registrationStatus,
			TrainingSessionStatus sessionStatus,
			LocalDate fromDate,
			LocalDate toDate,
			String keyword,
			int page,
			int limit) {
		return sessions(currentUserId, registrationStatus, sessionStatus, fromDate, toDate, keyword, page, limit, null, null);
	}

	@Transactional(readOnly = true)
	public Page<MyTrainingSessionResponse> sessions(
			Long currentUserId,
			TrainingRegistrationStatus registrationStatus,
			TrainingSessionStatus sessionStatus,
			LocalDate fromDate,
			LocalDate toDate,
			String keyword,
			int page,
			int limit,
			String sortBy,
			String order) {
		validateDateFilter(fromDate, toDate);
		PageRequest pageRequest = PageRequestFactory.create(
				page,
				limit,
				sortBy,
				order,
				Sort.by(Sort.Direction.ASC, "registeredAt"),
				"id", "registeredAt", "cancelledAt", "completedAt", "status");
		return trainingRegistrationRepository
				.searchMine(
						currentUserId,
						registrationStatus,
						sessionStatus,
						fromDate,
						toDate,
						normalize(keyword),
						pageRequest)
				.map(myTrainingMapper::toSessionResponse);
	}

	@Transactional(readOnly = true)
	public Page<MyAttendanceResponse> attendance(
			Long currentUserId,
			AttendanceStatus status,
			LocalDate fromDate,
			LocalDate toDate,
			String keyword,
			int page,
			int limit) {
		return attendance(currentUserId, status, fromDate, toDate, keyword, page, limit, null, null);
	}

	@Transactional(readOnly = true)
	public Page<MyAttendanceResponse> attendance(
			Long currentUserId,
			AttendanceStatus status,
			LocalDate fromDate,
			LocalDate toDate,
			String keyword,
			int page,
			int limit,
			String sortBy,
			String order) {
		validateDateFilter(fromDate, toDate);
		PageRequest pageRequest = PageRequestFactory.create(
				page,
				limit,
				sortBy,
				order,
				Sort.by(Sort.Direction.DESC, "createdAt"),
				"id", "createdAt", "updatedAt", "checkedInAt", "status");
		return attendanceRecordRepository
				.searchMine(
						currentUserId,
						status,
						fromDate,
						toDate,
						normalize(keyword),
						pageRequest)
				.map(myTrainingMapper::toAttendanceResponse);
	}

	@Transactional(readOnly = true)
	public MyTrainingDashboardResponse dashboard(Long currentUserId) {
		LocalDate today = LocalDate.now();
		long registeredSessions = trainingRegistrationRepository.countMine(
				currentUserId,
				TrainingRegistrationStatus.Registered,
				null,
				null,
				null);
		long upcomingSessions = trainingRegistrationRepository.countMine(
				currentUserId,
				TrainingRegistrationStatus.Registered,
				TrainingSessionStatus.Upcoming,
				today,
				null);
		long completedSessions = trainingRegistrationRepository.countMine(
				currentUserId,
				TrainingRegistrationStatus.Completed,
				null,
				null,
				null);
		long waitlistedSessions = trainingRegistrationRepository.countMine(
				currentUserId,
				TrainingRegistrationStatus.Waitlist,
				null,
				null,
				null);
		MyTrainingDashboardResponse.AttendanceSummary attendanceSummary = new MyTrainingDashboardResponse.AttendanceSummary(
				attendanceRecordRepository.countMine(currentUserId, AttendanceStatus.Present, null, null),
				attendanceRecordRepository.countMine(currentUserId, AttendanceStatus.Late, null, null),
				attendanceRecordRepository.countMine(currentUserId, AttendanceStatus.Absent, null, null));
		List<MyTrainingSessionResponse> nextSessions = trainingRegistrationRepository
				.searchMine(
						currentUserId,
						TrainingRegistrationStatus.Registered,
						TrainingSessionStatus.Upcoming,
						today,
						null,
						null,
						PageRequest.of(0, 5))
				.map(myTrainingMapper::toSessionResponse)
				.getContent();
		List<MyAttendanceResponse> recentAttendance = attendanceRecordRepository
				.searchMine(
						currentUserId,
						null,
						null,
						null,
						null,
						PageRequest.of(0, 5))
				.map(myTrainingMapper::toAttendanceResponse)
				.getContent();
		return new MyTrainingDashboardResponse(
				registeredSessions,
				upcomingSessions,
				completedSessions,
				waitlistedSessions,
				attendanceSummary,
				nextSessions,
				recentAttendance);
	}

	@Transactional(readOnly = true)
	public MyTrainerDashboardResponse trainerDashboard(Long currentUserId) {
		LocalDate today = LocalDate.now();
		long assignedClasses = classTrainerRepository.countDistinctClassesByTrainerId(currentUserId);
		long upcomingSessions = trainingSessionRepository.countByTrainerIdAndStatusAndSessionDateGreaterThanEqual(
				currentUserId,
				TrainingSessionStatus.Upcoming,
				today);
		long completedSessions = trainingSessionRepository.countByTrainerIdAndStatus(
				currentUserId,
				TrainingSessionStatus.Completed);
		long pendingAttendanceSessions = trainingSessionRepository.countPendingAttendanceSessions(
				currentUserId,
				TrainingSessionStatus.Upcoming,
				TrainingRegistrationStatus.Registered,
				today);
		List<TrainingSessionResponse> nextSessions = trainingSessionRepository
				.search(
						TrainingSessionStatus.Upcoming,
						null,
						currentUserId,
						today,
						null,
						null,
						PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "sessionDate", "startTime", "id")))
				.map(trainingSessionMapper::toResponse)
				.getContent();
		List<TrainingSessionResponse> recentCompletedSessions = trainingSessionRepository
				.search(
						TrainingSessionStatus.Completed,
						null,
						currentUserId,
						null,
						null,
						null,
						PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "sessionDate", "startTime", "id")))
				.map(trainingSessionMapper::toResponse)
				.getContent();
		return new MyTrainerDashboardResponse(
				assignedClasses,
				upcomingSessions,
				completedSessions,
				pendingAttendanceSessions,
				nextSessions,
				recentCompletedSessions);
	}

	@Transactional(readOnly = true)
	public MyClassAdminDashboardResponse classAdminDashboard(Long currentUserId) {
		LocalDate today = LocalDate.now();
		long assignedClasses = classRepository.countByAdminIdAndStatus(currentUserId, null);
		long activeClasses = classRepository.countByAdminIdAndStatus(currentUserId, ClassStatus.Active);
		long planningClasses = classRepository.countByAdminIdAndStatus(currentUserId, ClassStatus.Planning);
		long upcomingSessions = trainingSessionRepository.countByClassAdminId(
				currentUserId,
				TrainingSessionStatus.Upcoming,
				today,
				null);
		long pendingAttendanceSessions = trainingSessionRepository.countPendingAttendanceSessionsByClassAdminId(
				currentUserId,
				TrainingSessionStatus.Upcoming,
				TrainingRegistrationStatus.Registered,
				today);
		long totalTrainers = classTrainerRepository.countDistinctTrainersByClassAdminId(currentUserId);
		long totalParticipants = trainingRegistrationRepository.countByClassAdminIdAndStatus(
				currentUserId,
				TrainingRegistrationStatus.Registered);
		List<ClassResponse> classesStartingSoon = classRepository
				.searchByAdminId(
						currentUserId,
						null,
						today,
						null,
						PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "startDate", "id")))
				.map(classMapper::toResponse)
				.getContent();
		List<TrainingSessionResponse> recentSessions = trainingSessionRepository
				.searchByClassAdminId(
						currentUserId,
						null,
						null,
						null,
						PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "sessionDate", "startTime", "id")))
				.map(trainingSessionMapper::toResponse)
				.getContent();
		return new MyClassAdminDashboardResponse(
				assignedClasses,
				activeClasses,
				planningClasses,
				upcomingSessions,
				pendingAttendanceSessions,
				totalTrainers,
				totalParticipants,
				classesStartingSoon,
				recentSessions);
	}

	private void validateDateFilter(LocalDate fromDate, LocalDate toDate) {
		if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
			throw new BadRequestException("INVALID_MY_TRAINING_DATE_FILTER", "From date must be before or equal to to date");
		}
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
