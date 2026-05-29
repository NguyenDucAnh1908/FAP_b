package com.fap.settings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "system_settings")
public class SystemSetting {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "system_settings_seq")
	@SequenceGenerator(name = "system_settings_seq", sequenceName = "system_settings_seq", allocationSize = 1)
	private Long id;

	@Column(nullable = false, length = 50)
	private String category;

	@Column(name = "setting_key", nullable = false, length = 100)
	private String settingKey;

	@Lob
	@Column(name = "setting_value", nullable = false)
	private String settingValue;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}

