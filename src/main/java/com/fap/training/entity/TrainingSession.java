package com.fap.training.entity;

import com.fap.clazz.entity.FapClass;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.enums.TrainingSessionType;
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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "training_sessions")
@SQLRestriction("is_deleted = 0")
public class TrainingSession {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "training_sessions_seq")
	@SequenceGenerator(name = "training_sessions_seq", sequenceName = "training_sessions_seq", allocationSize = 1)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "class_id", nullable = false)
	private FapClass fapClass;

	@Column(nullable = false)
	private String title;

	@Lob
	@Column
	private String description;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trainer_id", nullable = false)
	private User trainer;

	@Column(length = 100)
	private String room;

	@Column(name = "session_date", nullable = false)
	private LocalDate sessionDate;

	@Column(name = "start_time", nullable = false)
	private LocalDateTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalDateTime endTime;

	@Enumerated(EnumType.STRING)
	@Column(name = "session_type", nullable = false, length = 20)
	private TrainingSessionType sessionType;

	@Column(name = "meeting_link", length = 512)
	private String meetingLink;

	@Column(nullable = false)
	private Integer capacity = 30;

	@Column(name = "enrolled_count", nullable = false)
	private Integer enrolledCount = 0;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TrainingSessionStatus status = TrainingSessionStatus.Upcoming;

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
