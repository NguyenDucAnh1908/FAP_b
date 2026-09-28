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
import org.hibernate.annotations.Formula;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "material_files")
public class MaterialFile {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "material_files_seq")
	@SequenceGenerator(name = "material_files_seq", sequenceName = "material_files_seq", allocationSize = 50)
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

	/**
	 * Whether bytes are stored in {@code material_file_contents}. Read with the row as a primary-key
	 * EXISTS, so listing materials neither loads the BLOB nor runs a query per material. It is not
	 * written back; code that stores content in the same transaction sets it for the response.
	 */
	@Formula("(case when exists (select 1 from material_file_contents c where c.material_file_id = id) then 1 else 0 end)")
	private boolean contentStored;
}
