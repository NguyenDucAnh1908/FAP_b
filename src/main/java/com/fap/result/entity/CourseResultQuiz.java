package com.fap.result.entity;

import com.fap.quiz.entity.Quiz;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Getter
@Setter
@Entity
@Table(name = "course_result_quizzes")
public class CourseResultQuiz {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "course_result_quizzes_seq")
	@SequenceGenerator(name = "course_result_quizzes_seq", sequenceName = "course_result_quizzes_seq", allocationSize = 1)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "course_result_id", nullable = false)
	private CourseResult courseResult;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_id", nullable = false)
	private Quiz quiz;

	@Column(name = "required_score", nullable = false)
	private Integer requiredScore;

	@Column(name = "best_attempt_id")
	private Long bestAttemptId;

	@Column(name = "best_score")
	private Integer bestScore;

	@Column(nullable = false)
	private boolean passed;
}
