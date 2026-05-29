package com.fap.settings.service;

import com.fap.common.audit.AuditLogService;
import com.fap.settings.dto.SettingsResponse;
import com.fap.settings.dto.UpdateSettingsRequest;
import com.fap.settings.entity.SystemSetting;
import com.fap.settings.repository.SystemSettingRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettingsServiceTest {

	private final SystemSettingRepository repository = mock(SystemSettingRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final SettingsService service = new SettingsService(repository, auditLogService);

	@Test
	void getAllReturnsGroupedSettings() {
		SystemSetting setting1 = createSetting("notification", "email_enabled", "true");
		SystemSetting setting2 = createSetting("notification", "sms_enabled", "false");
		SystemSetting setting3 = createSetting("general", "site_name", "FAP");
		when(repository.findAll()).thenReturn(List.of(setting1, setting2, setting3));

		SettingsResponse response = service.getAll();

		assertThat(response.settings()).containsOnlyKeys("notification", "general");
		assertThat(response.settings().get("notification")).containsEntry("email_enabled", "true");
		assertThat(response.settings().get("notification")).containsEntry("sms_enabled", "false");
		assertThat(response.settings().get("general")).containsEntry("site_name", "FAP");
	}

	@Test
	void getByCategoryReturnsOnlyMatchingSettings() {
		SystemSetting setting1 = createSetting("notification", "email_enabled", "true");
		SystemSetting setting2 = createSetting("notification", "sms_enabled", "false");
		when(repository.findByCategoryOrderBySettingKeyAsc("notification")).thenReturn(List.of(setting1, setting2));

		SettingsResponse response = service.getByCategory("notification");

		assertThat(response.settings()).containsOnlyKeys("notification");
		assertThat(response.settings().get("notification")).hasSize(2);
	}

	@Test
	void updateCreatesNewSettingWhenNotExists() {
		when(repository.findByCategoryAndSettingKey("test", "key")).thenReturn(Optional.empty());
		when(repository.save(any(SystemSetting.class))).thenAnswer(inv -> inv.getArgument(0));
		when(repository.findAll()).thenReturn(List.of());

		UpdateSettingsRequest request = new UpdateSettingsRequest(
				Map.of("test", Map.of("key", "value")));
		service.update(request);

		verify(repository).save(any(SystemSetting.class));
		verify(auditLogService).record("UPDATE_SETTINGS", "system_settings", null);
	}

	@Test
	void updateUpdatesExistingSettingValue() {
		SystemSetting existing = createSetting("test", "key", "old-value");
		when(repository.findByCategoryAndSettingKey("test", "key")).thenReturn(Optional.of(existing));
		when(repository.save(any(SystemSetting.class))).thenAnswer(inv -> inv.getArgument(0));
		when(repository.findAll()).thenReturn(List.of(existing));

		UpdateSettingsRequest request = new UpdateSettingsRequest(
				Map.of("test", Map.of("key", "new-value")));
		service.update(request);

		assertThat(existing.getSettingValue()).isEqualTo("new-value");
	}

	@Test
	void getValueReturnsStoredValueOrDefault() {
		when(repository.findByCategoryAndSettingKey("test", "exists"))
				.thenReturn(Optional.of(createSetting("test", "exists", "stored")));
		when(repository.findByCategoryAndSettingKey("test", "missing"))
				.thenReturn(Optional.empty());

		assertThat(service.getValue("test", "exists", "default")).isEqualTo("stored");
		assertThat(service.getValue("test", "missing", "default")).isEqualTo("default");
	}

	private SystemSetting createSetting(String category, String key, String value) {
		SystemSetting setting = new SystemSetting();
		setting.setCategory(category);
		setting.setSettingKey(key);
		setting.setSettingValue(value);
		setting.setCreatedAt(LocalDateTime.now());
		setting.setUpdatedAt(LocalDateTime.now());
		return setting;
	}
}

