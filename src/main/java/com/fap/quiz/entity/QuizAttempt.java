package com.fap.quiz.entity;

import com.fap.quiz.enums.QuizAttemptStatus;
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

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "quiz_attempts_seq")
	@SequenceGenerator(name = "quiz_attempts_seq", sequenceName = "quiz_attempts_seq", allocationSize = 1)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_id", nullable = false)
	private Quiz quiz;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "attempt_number", nullable = false)
	private Integer attemptNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private QuizAttemptStatus status = QuizAttemptStatus.InProgress;

	@Lob
	@Column(name = "answers_json", nullable = false)
	private String answersJson;

	@Column
	private Integer score;

	@Column(name = "correct_count")
	private Integer correctCount;

	@Column(name = "total_questions")
	private Integer totalQuestions;

	@Column
	private Boolean passed;

	@Column(name = "time_taken_seconds")
	private Integer timeTakenSeconds;

	@Column(name = "started_at", nullable = false)
	private LocalDateTime startedAt;

	@Column(name = "submitted_at")
	private LocalDateTime submittedAt;

	@Version
	@Column(name = "version_no", nullable = false)
	private Long versionNo;
}
