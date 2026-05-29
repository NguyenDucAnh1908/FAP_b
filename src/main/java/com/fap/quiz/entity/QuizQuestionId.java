package com.fap.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Embeddable
public class QuizQuestionId implements Serializable {

	@Column(name = "quiz_id")
	private Long quizId;

	@Column(name = "question_id")
	private Long questionId;

	public QuizQuestionId(Long quizId, Long questionId) {
		this.quizId = quizId;
		this.questionId = questionId;
	}
}
