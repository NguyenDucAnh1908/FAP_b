package com.fap.common.security;

/**
 * Names of the four seeded roles (migration V2). They are compared as exact strings against
 * {@code roles.name} and {@link FapUserPrincipal#roles()}, and leave the backend in the JWT
 * {@code roles} claim and API responses, so the values must never change.
 *
 * <p>Compile-time constants, so they are usable in annotation values and {@code case} labels.
 */
public final class RoleNames {

	public static final String SUPER_ADMIN = "Super Admin";
	public static final String CLASS_ADMIN = "Class Admin";
	public static final String TRAINER = "Trainer";
	public static final String TRAINEE = "Trainee";

	private RoleNames() {
	}
}
