package com.fap.clazz.entity;

import com.fap.syllabus.entity.Syllabus;
import com.fap.user.entity.User;
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
@Table(name = "class_trainers")
public class ClassTrainer {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "class_trainers_seq")
	@SequenceGenerator(name = "class_trainers_seq", sequenceName = "class_trainers_seq", allocationSize = 1)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "class_id", nullable = false)
	private FapClass fapClass;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "syllabus_id")
	private Syllabus syllabus;
}
