package com.fap.quiz.entity;

import com.fap.clazz.entity.FapClass;
import com.fap.training.entity.TrainingSession;
import com.fap.user.entity.User;
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

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "quiz_assignments")
public class QuizAssignment {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "quiz_assignments_seq")
	@SequenceGenerator(name = "quiz_assignments_seq", sequenceName = "quiz_assignments_seq", allocationSize = 1)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_id", nullable = false)
	private Quiz quiz;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "class_id")
	private FapClass fapClass;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "training_session_id")
	private TrainingSession trainingSession;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "assigned_by", nullable = false)
	private User assignedBy;

	@Column(name = "assigned_at", nullable = false)
	private LocalDateTime assignedAt;
}
