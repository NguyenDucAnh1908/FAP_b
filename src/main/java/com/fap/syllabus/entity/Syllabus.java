package com.fap.syllabus.entity;

import com.fap.syllabus.enums.SyllabusStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "syllabuses")
@SQLRestriction("is_deleted = 0")
public class Syllabus {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "syllabuses_seq")
	@SequenceGenerator(name = "syllabuses_seq", sequenceName = "syllabuses_seq", allocationSize = 1)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true, length = 50)
	private String code;

	@Column(nullable = false, length = 20)
	private String version = "v1.0";

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SyllabusStatus status = SyllabusStatus.Drafting;

	@Column(name = "level_name", nullable = false, length = 30)
	private String levelName = "All levels";

	@Column(nullable = false)
	private Integer attendees = 30;

	@Column(length = 50)
	private String duration;

	@Lob
	@Column(name = "technical_requirements")
	private String technicalRequirements;

	@Lob
	@Column(name = "course_objectives")
	private String courseObjectives;

	@Lob
	private String rules;

	@Column(name = "time_alloc_assignment_lab", nullable = false)
	private Integer timeAllocAssignmentLab = 50;

	@Column(name = "time_alloc_concept_lecture", nullable = false)
	private Integer timeAllocConceptLecture = 30;

	@Column(name = "time_alloc_guide_review", nullable = false)
	private Integer timeAllocGuideReview = 10;

	@Column(name = "time_alloc_test_quiz", nullable = false)
	private Integer timeAllocTestQuiz = 10;

	@Column(name = "assess_quiz_pct", nullable = false)
	private Integer assessQuizPct = 15;

	@Column(name = "assess_assignment_pct", nullable = false)
	private Integer assessAssignmentPct = 15;

	@Column(name = "assess_final_pct", nullable = false)
	private Integer assessFinalPct = 70;

	@Lob
	@Column(name = "assessment_text")
	private String assessmentText;

	@Column(name = "is_deleted", nullable = false)
	private boolean deleted;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Version
	@Column(name = "version_no", nullable = false)
	private Long versionNo;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "created_by")
	private Long createdBy;

	@Column(name = "updated_by")
	private Long updatedBy;
}
