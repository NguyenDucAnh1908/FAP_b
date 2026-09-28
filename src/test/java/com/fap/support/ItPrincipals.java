package com.fap.support;

import com.fap.common.security.FapUserPrincipal;
import com.fap.common.security.RoleNames;

import java.util.List;
import java.util.Set;

public final class ItPrincipals {

	private ItPrincipals() {
	}

	public static FapUserPrincipal superAdmin(Long id) {
		return new FapUserPrincipal(id, "it-super-admin@fap.local", "", Set.of(RoleNames.SUPER_ADMIN), true, List.of());
	}
}
