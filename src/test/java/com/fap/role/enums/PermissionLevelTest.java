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
}
