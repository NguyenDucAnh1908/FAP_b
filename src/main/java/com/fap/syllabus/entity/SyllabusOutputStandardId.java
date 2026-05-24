package com.fap.syllabus.entity;

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
public class SyllabusOutputStandardId implements Serializable {

	@Column(name = "syllabus_id")
	private Long syllabusId;

	@Column(name = "standard_code", length = 10)
	private String standardCode;

	public SyllabusOutputStandardId(Long syllabusId, String standardCode) {
		this.syllabusId = syllabusId;
		this.standardCode = standardCode;
	}
}
