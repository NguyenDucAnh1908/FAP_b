package com.fap.clazz.entity;

import com.fap.clazz.enums.ClassStatus;
import com.fap.program.entity.TrainingProgram;
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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "classes")
@SQLRestriction("is_deleted = 0")
public class FapClass {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "classes_seq")
	@SequenceGenerator(name = "classes_seq", sequenceName = "classes_seq", allocationSize = 1)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(name = "class_code", nullable = false, unique = true, length = 100)
	private String classCode;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "training_program_id", nullable = false)
	private TrainingProgram trainingProgram;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ClassStatus status = ClassStatus.Planning;

	@Column(length = 100)
	private String location;

	@Column(name = "location_detail")
	private String locationDetail;

	@Column(length = 20)
	private String fsu;

	@Column(name = "class_time", length = 50)
	private String classTime;

	@Column(name = "start_date")
	private LocalDate startDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	@Column(length = 50)
	private String duration;

	@Column(nullable = false)
	private Integer capacity = 30;

	@Column(name = "self_enrollment_enabled", nullable = false)
	private boolean selfEnrollmentEnabled;

	@Column(name = "enrollment_start_date")
	private LocalDate enrollmentStartDate;

	@Column(name = "enrollment_end_date")
	private LocalDate enrollmentEndDate;

	@Column(name = "minimum_attendance_rate", nullable = false, precision = 5, scale = 2)
	private BigDecimal minimumAttendanceRate = BigDecimal.valueOf(80);

	@Column(name = "is_deleted", nullable = false)
	private boolean deleted;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Version
	@Column(name = "version_no", nullable = false)
	private Long versionNo;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "created_by")
	private Long createdBy;

	@Column(name = "updated_by")
	private Long updatedBy;
}
