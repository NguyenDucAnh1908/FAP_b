package com.fap.result.entity;

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
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "course_result_adjustments")
public class CourseResultAdjustment {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "course_result_adjustments_seq")
	@SequenceGenerator(name = "course_result_adjustments_seq", sequenceName = "course_result_adjustments_seq", allocationSize = 1)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "course_result_id", nullable = false)
	private CourseResult courseResult;

	@Enumerated(EnumType.STRING)
	@Column(name = "previous_status", nullable = false, length = 20)
	private CourseResultStatus previousStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "new_status", nullable = false, length = 20)
	private CourseResultStatus newStatus;

	@Column(nullable = false, length = 1000)
	private String reason;

	@Column(name = "adjusted_by", nullable = false)
	private Long adjustedBy;

	@Column(name = "adjusted_at", nullable = false)
	private LocalDateTime adjustedAt;
}
