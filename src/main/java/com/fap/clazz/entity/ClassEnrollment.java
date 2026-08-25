package com.fap.clazz.entity;

import com.fap.clazz.enums.ClassEnrollmentSource;
import com.fap.clazz.enums.ClassEnrollmentStatus;
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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "class_enrollments")
public class ClassEnrollment {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "class_enrollments_seq")
	@SequenceGenerator(name = "class_enrollments_seq", sequenceName = "class_enrollments_seq", allocationSize = 1)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "class_id", nullable = false)
	private FapClass fapClass;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ClassEnrollmentStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ClassEnrollmentSource source;

	@Column(name = "enrolled_at")
	private LocalDateTime enrolledAt;

	@Column(name = "withdrawn_at")
	private LocalDateTime withdrawnAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "reviewed_at")
	private LocalDateTime reviewedAt;

	@Column(name = "reviewed_by")
	private Long reviewedBy;

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
