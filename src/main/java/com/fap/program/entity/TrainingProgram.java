package com.fap.program.entity;

import com.fap.program.enums.TrainingProgramStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "training_programs")
@SQLRestriction("is_deleted = 0")
public class TrainingProgram {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "training_programs_seq")
	@SequenceGenerator(name = "training_programs_seq", sequenceName = "training_programs_seq", allocationSize = 1)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TrainingProgramStatus status = TrainingProgramStatus.Planning;

	@Column(length = 50)
	private String duration;

	@Column(name = "total_hours")
	private Integer totalHours;

	@Column(length = 20)
	private String version = "v1.0";

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
