package com.fap.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {

	@EmbeddedId
	private QuizQuestionId id;

	@MapsId("quizId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_id", nullable = false)
	private Quiz quiz;

	@MapsId("questionId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id", nullable = false)
	private Question question;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal points = BigDecimal.ONE;
}
