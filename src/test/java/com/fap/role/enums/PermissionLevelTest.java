package com.fap.role.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionLevelTest {

	@Test
	void permissionLevelsMapToActionsWithoutOrdinalComparison() {
		assertThat(PermissionLevel.access_denied.allows("read")).isFalse();
		assertThat(PermissionLevel.view.allows("read")).isTrue();
		assertThat(PermissionLevel.view.allows("create")).isFalse();
		assertThat(PermissionLevel.create.allows("create")).isTrue();
		assertThat(PermissionLevel.create.allows("update")).isFalse();
		assertThat(PermissionLevel.modify.allows("transition")).isTrue();
		assertThat(PermissionLevel.modify.allows("delete")).isFalse();
		assertThat(PermissionLevel.full_access.allows("admin")).isTrue();
	}

	@Test
	void permissionLevelsCanCheckRequiredLevels() {
		assertThat(PermissionLevel.view.allows(PermissionLevel.view)).isTrue();
		assertThat(PermissionLevel.view.allows(PermissionLevel.create)).isFalse();
		assertThat(PermissionLevel.create.allows(PermissionLevel.view)).isTrue();
		assertThat(PermissionLevel.create.allows(PermissionLevel.modify)).isFalse();
		assertThat(PermissionLevel.modify.allows(PermissionLevel.view)).isTrue();
		assertThat(PermissionLevel.modify.allows(PermissionLevel.create)).isFalse();
		assertThat(PermissionLevel.full_access.allows(PermissionLevel.modify)).isTrue();
		assertThat(PermissionLevel.full_access.allows(PermissionLevel.full_access)).isTrue();
	}
}
