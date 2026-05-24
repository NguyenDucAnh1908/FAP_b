package com.fap.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.Set;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

	private static final Set<String> COMMON_PASSWORDS = Set.of(
			"password",
			"password1",
			"password123",
			"12345678",
			"123456789",
			"qwerty123",
			"admin123",
			"admin1234");

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return true;
		}
		String normalized = value.toLowerCase(Locale.ROOT);
		return value.length() >= 8
				&& value.length() <= 100
				&& !COMMON_PASSWORDS.contains(normalized)
				&& value.chars().anyMatch(Character::isUpperCase)
				&& value.chars().anyMatch(Character::isLowerCase)
				&& value.chars().anyMatch(Character::isDigit);
	}
}
