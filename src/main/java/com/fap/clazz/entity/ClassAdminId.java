package com.fap.clazz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ClassAdminId implements Serializable {

	@Column(name = "class_id")
	private Long classId;

	@Column(name = "user_id")
	private Long userId;

	public ClassAdminId(Long classId, Long userId) {
		this.classId = classId;
		this.userId = userId;
	}
}
