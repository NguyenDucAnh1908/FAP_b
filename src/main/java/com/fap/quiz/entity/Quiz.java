package com.fap.quiz.entity;

import com.fap.quiz.enums.QuizStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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
@Table(name = "quizzes")
@SQLRestriction("is_deleted = 0")
public class Quiz {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "quizzes_seq")
	@SequenceGenerator(name = "quizzes_seq", sequenceName = "quizzes_seq", allocationSize = 1)
	private Long id;

	@Column(nullable = false, length = 255)
	private String title;

	@Lob
	@Column
	private String description;

	@Column(name = "duration_minutes", nullable = false)
	private Integer durationMinutes;

	@Column(name = "passing_score", nullable = false)
	private Integer passingScore;

	@Column(name = "max_attempts", nullable = false)
	private Integer maxAttempts = 1;

	@Column(nullable = false)
	private boolean randomize;

	@Column(nullable = false, length = 100)
	private String category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private QuizStatus status = QuizStatus.Draft;

	@Column(name = "open_date")
	private LocalDate openDate;

	@Column(name = "close_date")
	private LocalDate closeDate;

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
