package com.fap.quiz.entity;

import com.fap.quiz.enums.QuestionDifficulty;
import com.fap.quiz.enums.QuestionType;
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
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "questions")
@SQLRestriction("is_deleted = 0")
public class Question {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "questions_seq")
	@SequenceGenerator(name = "questions_seq", sequenceName = "questions_seq", allocationSize = 1)
	private Long id;

	@Lob
	@Column(nullable = false)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "question_type", nullable = false, length = 20)
	private QuestionType questionType;

	@Column(nullable = false, length = 100)
	private String category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private QuestionDifficulty difficulty;

	@Lob
	@Column(name = "options_json", nullable = false)
	private String optionsJson;

	@Lob
	@Column(name = "correct_answers_json", nullable = false)
	private String correctAnswersJson;

	@Lob
	@Column
	private String explanation;

	@Column(name = "is_deleted", nullable = false)
	private boolean deleted;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "created_by")
	private Long createdBy;

	@Column(name = "updated_by")
	private Long updatedBy;
}
