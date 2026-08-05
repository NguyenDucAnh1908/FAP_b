package com.fap.syllabus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Bytes of an internally uploaded material, split out of {@link MaterialFile} so that list queries
 * never load the BLOB. The primary key is shared with the owning material file via {@link MapsId},
 * so there is no sequence of its own and no chance of the two rows drifting apart.
 */
@Getter
@Setter
@Entity
@Table(name = "material_file_contents")
public class MaterialFileContent {

	@Id
	@Column(name = "material_file_id")
	private Long materialFileId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "material_file_id")
	private MaterialFile materialFile;

	@Lob
	@Column(name = "file_data", nullable = false)
	private byte[] fileData;
}
