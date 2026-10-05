package com.fap.user.entity;

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
 * Avatar bytes for a user, split into a separate table so that user list and profile queries never
 * load the BLOB. The primary key is shared with {@link User} via {@link MapsId}.
 */
@Getter
@Setter
@Entity
@Table(name = "user_avatar_contents")
public class UserAvatarContent {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private User user;

	@Lob
	@Column(name = "file_data", nullable = false)
	private byte[] fileData;

	@Column(name = "content_type", nullable = false, length = 100)
	private String contentType;
}
