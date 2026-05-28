package com.fap.training.entity;

import com.fap.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "training_feedbacks")
public class TrainingFeedback {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "training_feedbacks_seq")
	@SequenceGenerator(name = "training_feedbacks_seq", sequenceName = "training_feedbacks_seq", allocationSize = 1)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "training_id", nullable = false)
	private TrainingSession trainingSession;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "rating_content", nullable = false)
	private Integer ratingContent;

	@Column(name = "rating_trainer", nullable = false)
	private Integer ratingTrainer;

	@Column(name = "rating_organization", nullable = false)
	private Integer ratingOrganization;

	@Lob
	@Column(name = "feedback_comment")
	private String comment;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}
