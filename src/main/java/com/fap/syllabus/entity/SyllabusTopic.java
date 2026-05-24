package com.fap.syllabus.entity;

import com.fap.syllabus.enums.SyllabusTopicStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "syllabus_topics")
public class SyllabusTopic {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "syllabus_topics_seq")
	@SequenceGenerator(name = "syllabus_topics_seq", sequenceName = "syllabus_topics_seq", allocationSize = 1)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "unit_id", nullable = false)
	private SyllabusUnit unit;

	@Column(nullable = false)
	private String name;

	@Column(name = "output_standard", nullable = false, length = 10)
	private String outputStandard;

	@Column(name = "is_online", nullable = false)
	private boolean online = true;

	@Column(name = "duration_minutes", nullable = false)
	private Integer durationMinutes = 30;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SyllabusTopicStatus status = SyllabusTopicStatus.Active;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;
}
