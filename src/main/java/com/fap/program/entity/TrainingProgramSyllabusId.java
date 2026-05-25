package com.fap.program.entity;

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
public class TrainingProgramSyllabusId implements Serializable {

	@Column(name = "program_id")
	private Long programId;

	@Column(name = "syllabus_id")
	private Long syllabusId;

	public TrainingProgramSyllabusId(Long programId, Long syllabusId) {
		this.programId = programId;
		this.syllabusId = syllabusId;
	}
}
