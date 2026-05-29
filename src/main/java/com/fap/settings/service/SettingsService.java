package com.fap.settings.service;

import com.fap.common.audit.AuditLogService;
import com.fap.settings.dto.SettingsResponse;
import com.fap.settings.dto.UpdateSettingsRequest;
import com.fap.settings.entity.SystemSetting;
import com.fap.settings.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SettingsService {

	private final SystemSettingRepository systemSettingRepository;
	private final AuditLogService auditLogService;

	public SettingsService(
			SystemSettingRepository systemSettingRepository,
			AuditLogService auditLogService) {
		this.systemSettingRepository = systemSettingRepository;
		this.auditLogService = auditLogService;
	}

	@Transactional(readOnly = true)
	public SettingsResponse getAll() {
		List<SystemSetting> settings = systemSettingRepository.findAll();
		Map<String, Map<String, String>> grouped = new LinkedHashMap<>();
		for (SystemSetting setting : settings) {
			grouped.computeIfAbsent(setting.getCategory(), k -> new LinkedHashMap<>())
					.put(setting.getSettingKey(), setting.getSettingValue());
		}
		return new SettingsResponse(grouped);
	}

	@Transactional(readOnly = true)
	public SettingsResponse getByCategory(String category) {
		List<SystemSetting> settings = systemSettingRepository.findByCategoryOrderBySettingKeyAsc(category);
		Map<String, Map<String, String>> grouped = new LinkedHashMap<>();
		Map<String, String> categorySettings = new LinkedHashMap<>();
		for (SystemSetting setting : settings) {
			categorySettings.put(setting.getSettingKey(), setting.getSettingValue());
		}
		if (!categorySettings.isEmpty()) {
			grouped.put(category, categorySettings);
		}
		return new SettingsResponse(grouped);
	}

	@Transactional
	public SettingsResponse update(UpdateSettingsRequest request) {
		LocalDateTime now = LocalDateTime.now();
		for (Map.Entry<String, Map<String, String>> categoryEntry : request.settings().entrySet()) {
			String category = categoryEntry.getKey();
			for (Map.Entry<String, String> settingEntry : categoryEntry.getValue().entrySet()) {
				String key = settingEntry.getKey();
				String value = settingEntry.getValue();
				SystemSetting setting = systemSettingRepository.findByCategoryAndSettingKey(category, key)
						.orElseGet(() -> {
							SystemSetting newSetting = new SystemSetting();
							newSetting.setCategory(category);
							newSetting.setSettingKey(key);
							newSetting.setCreatedAt(now);
							return newSetting;
						});
				setting.setSettingValue(value);
				setting.setUpdatedAt(now);
				systemSettingRepository.save(setting);
			}
		}
		auditLogService.record("UPDATE_SETTINGS", "system_settings", null);
		return getAll();
	}

	@Transactional(readOnly = true)
	public String getValue(String category, String key, String defaultValue) {
		return systemSettingRepository.findByCategoryAndSettingKey(category, key)
				.map(SystemSetting::getSettingValue)
				.orElse(defaultValue);
	}
}

