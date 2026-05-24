package com.fap.syllabus.entity;

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
@Table(name = "syllabus_output_standards")
public class SyllabusOutputStandard {

	@EmbeddedId
	private SyllabusOutputStandardId id;

	@MapsId("syllabusId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "syllabus_id", nullable = false)
	private Syllabus syllabus;

	public String getStandardCode() {
		return id.getStandardCode();
	}
}
