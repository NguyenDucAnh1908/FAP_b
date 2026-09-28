package com.fap.support;

import com.fap.common.security.FapUserPrincipal;

import java.util.List;
import java.util.Set;

public final class ItPrincipals {

	private ItPrincipals() {
	}

	public static FapUserPrincipal superAdmin(Long id) {
		return new FapUserPrincipal(id, "it-super-admin@fap.local", "", Set.of("Super Admin"), true, List.of());
	}
}
