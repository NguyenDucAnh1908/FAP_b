package com.fap.role.enums;

import java.util.Set;

public enum PermissionLevel {
	access_denied(Set.of()),
	view(Set.of("read")),
	create(Set.of("read", "create")),
	modify(Set.of("read", "update", "transition")),
	full_access(Set.of("read", "create", "update", "transition", "delete", "admin"));

	private final Set<String> actions;

	PermissionLevel(Set<String> actions) {
		this.actions = actions;
	}

	public boolean allows(String action) {
		return actions.contains(action);
	}
}
