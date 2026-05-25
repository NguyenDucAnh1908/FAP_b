package com.fap.program.entity;

import com.fap.syllabus.entity.Syllabus;
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

@Getter
@Setter
@Entity
@Table(name = "training_program_syllabuses")
public class TrainingProgramSyllabus {

	@EmbeddedId
	private TrainingProgramSyllabusId id;

	@MapsId("programId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "program_id", nullable = false)
	private TrainingProgram program;

	@MapsId("syllabusId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "syllabus_id", nullable = false)
	private Syllabus syllabus;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;
}
