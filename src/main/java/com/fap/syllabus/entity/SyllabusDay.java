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
@Table(name = "syllabus_days")
public class SyllabusDay {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "syllabus_days_seq")
	@SequenceGenerator(name = "syllabus_days_seq", sequenceName = "syllabus_days_seq", allocationSize = 1)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "syllabus_id", nullable = false)
	private Syllabus syllabus;

	@Column(name = "day_number", nullable = false)
	private Integer dayNumber;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	@OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC")
	private List<SyllabusUnit> units = new ArrayList<>();
}
