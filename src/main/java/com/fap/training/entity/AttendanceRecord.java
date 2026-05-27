package com.fap.training.entity;

import com.fap.training.enums.AttendanceCheckInMethod;
import com.fap.training.enums.AttendanceStatus;
import com.fap.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attendance_records_seq")
	@SequenceGenerator(name = "attendance_records_seq", sequenceName = "attendance_records_seq", allocationSize = 1)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "training_id", nullable = false)
	private TrainingSession trainingSession;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AttendanceStatus status;

	@Column(name = "checked_in_at")
	private LocalDateTime checkedInAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "check_in_method", nullable = false, length = 20)
	private AttendanceCheckInMethod checkInMethod = AttendanceCheckInMethod.Manual;

	@Column(name = "updated_by")
	private Long updatedBy;

	@Column(name = "correction_reason", length = 500)
	private String correctionReason;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}
