package com.fap.syllabus.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "syllabus_units")
public class SyllabusUnit {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "syllabus_units_seq")
	@SequenceGenerator(name = "syllabus_units_seq", sequenceName = "syllabus_units_seq", allocationSize = 1)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "day_id", nullable = false)
	private SyllabusDay day;

	@Column(nullable = false)
	private String name;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	@OneToMany(mappedBy = "unit", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC")
	private List<SyllabusTopic> topics = new ArrayList<>();
}
