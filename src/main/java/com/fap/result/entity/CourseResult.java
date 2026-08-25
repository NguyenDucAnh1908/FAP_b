package com.fap.result.entity;

import com.fap.clazz.entity.ClassEnrollment;
import com.fap.clazz.entity.FapClass;
import com.fap.result.enums.CourseResultStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "course_results")
public class CourseResult {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "course_results_seq")
	@SequenceGenerator(name = "course_results_seq", sequenceName = "course_results_seq", allocationSize = 1)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "class_id", nullable = false)
	private FapClass fapClass;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "class_enrollment_id", nullable = false)
	private ClassEnrollment classEnrollment;

	@Enumerated(EnumType.STRING)
	@Column(name = "calculated_status", nullable = false, length = 20)
	private CourseResultStatus calculatedStatus = CourseResultStatus.InProgress;

	@Enumerated(EnumType.STRING)
	@Column(name = "override_status", length = 20)
	private CourseResultStatus overrideStatus;

	@Column(name = "attendance_rate", nullable = false, precision = 5, scale = 2)
	private BigDecimal attendanceRate = BigDecimal.ZERO;

	@Column(name = "attended_sessions", nullable = false)
	private Integer attendedSessions = 0;

	@Column(name = "total_sessions", nullable = false)
	private Integer totalSessions = 0;

	@Column(name = "required_quiz_count", nullable = false)
	private Integer requiredQuizCount = 0;

	@Column(name = "passed_quiz_count", nullable = false)
	private Integer passedQuizCount = 0;

	@Column(name = "calculated_at")
	private LocalDateTime calculatedAt;

	@Column(name = "calculated_by")
	private Long calculatedBy;

	@Column(name = "override_reason", length = 1000)
	private String overrideReason;

	@Column(name = "overridden_at")
	private LocalDateTime overriddenAt;

	@Column(name = "overridden_by")
	private Long overriddenBy;

	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	@Column(name = "published_by")
	private Long publishedBy;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Version
	@Column(name = "version_no", nullable = false)
	private Long versionNo;

	public CourseResultStatus effectiveStatus() {
		return overrideStatus == null ? calculatedStatus : overrideStatus;
	}
}
