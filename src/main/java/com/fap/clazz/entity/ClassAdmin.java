package com.fap.clazz.entity;

import com.fap.user.entity.User;
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
@Table(name = "class_admins")
public class ClassAdmin {

	@EmbeddedId
	private ClassAdminId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("classId")
	@JoinColumn(name = "class_id", nullable = false)
	private FapClass fapClass;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("userId")
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
}
