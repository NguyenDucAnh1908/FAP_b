package com.fap.syllabus.entity;

import jakarta.persistence.Column;
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

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "material_files")
public class MaterialFile {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "material_files_seq")
	@SequenceGenerator(name = "material_files_seq", sequenceName = "material_files_seq", allocationSize = 1)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "topic_id", nullable = false)
	private SyllabusTopic topic;

	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Column(name = "file_url", nullable = false, length = 512)
	private String fileUrl;

	@Column(name = "file_size")
	private Long fileSize;

	@Column(name = "content_type", length = 100)
	private String contentType;

	@Column(name = "uploaded_by")
	private Long uploadedBy;

	@Column(name = "uploaded_at", nullable = false)
	private LocalDateTime uploadedAt;
}
