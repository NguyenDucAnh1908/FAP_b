package com.fap.training.service;

import com.fap.common.exception.BadRequestException;
import com.fap.training.dto.MyAttendanceResponse;
import com.fap.training.dto.MyTrainingRegistrationResponse;
import com.fap.training.dto.MyTrainingSessionResponse;
import com.fap.training.enums.AttendanceStatus;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.mapper.MyTrainingMapper;
import com.fap.training.repository.AttendanceRecordRepository;
import com.fap.training.repository.TrainingRegistrationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class MyTrainingService {

	private final TrainingRegistrationRepository trainingRegistrationRepository;
	private final AttendanceRecordRepository attendanceRecordRepository;
	private final MyTrainingMapper myTrainingMapper;

	public MyTrainingService(
			TrainingRegistrationRepository trainingRegistrationRepository,
			AttendanceRecordRepository attendanceRecordRepository,
			MyTrainingMapper myTrainingMapper) {
		this.trainingRegistrationRepository = trainingRegistrationRepository;
		this.attendanceRecordRepository = attendanceRecordRepository;
		this.myTrainingMapper = myTrainingMapper;
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
		validateDateFilter(fromDate, toDate);
		return trainingRegistrationRepository
				.searchMine(
						currentUserId,
						registrationStatus,
						sessionStatus,
						fromDate,
						toDate,
						normalize(keyword),
						PageRequest.of(page, limit))
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
		validateDateFilter(fromDate, toDate);
		return trainingRegistrationRepository
				.searchMine(
						currentUserId,
						registrationStatus,
						sessionStatus,
						fromDate,
						toDate,
						normalize(keyword),
						PageRequest.of(page, limit))
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
		validateDateFilter(fromDate, toDate);
		return attendanceRecordRepository
				.searchMine(
						currentUserId,
						status,
						fromDate,
						toDate,
						normalize(keyword),
						PageRequest.of(page, limit))
				.map(myTrainingMapper::toAttendanceResponse);
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
