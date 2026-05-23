package com.fap.user.service;

import com.fap.common.exception.ConflictException;
import com.fap.role.entity.Role;
import com.fap.role.repository.RoleRepository;
import com.fap.user.dto.CreateUserRequest;
import com.fap.user.dto.UserResponse;
import com.fap.user.enums.Gender;
import com.fap.user.mapper.UserMapper;
import com.fap.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final RoleRepository roleRepository = mock(RoleRepository.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final UserMapper userMapper = mock(UserMapper.class);
	private final UserService userService = new UserService(userRepository, roleRepository, passwordEncoder, userMapper);

	@Test
	void createHashesPasswordAndMapsResponse() {
		Role role = new Role();
		role.setId(1L);
		when(userRepository.existsByEmailIgnoreCase("USER@Example.com")).thenReturn(false);
		when(roleRepository.findByIdIn(Set.of(1L))).thenReturn(Set.of(role));
		when(passwordEncoder.encode("password123")).thenReturn("hash");
		when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		UserResponse expected = new UserResponse(null, "User", "user@example.com", null, null, Gender.Male, null, null, null, null, null);
		when(userMapper.toResponse(any())).thenReturn(expected);

		UserResponse response = userService.create(new CreateUserRequest(
				"User",
				"USER@Example.com",
				null,
				"password123",
				null,
				Gender.Male,
				null,
				Set.of(1L)));

		assertThat(response).isEqualTo(expected);
		verify(passwordEncoder).encode("password123");
	}

	@Test
	void createRejectsDuplicateEmail() {
		when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

		assertThatThrownBy(() -> userService.create(new CreateUserRequest(
				"User",
				"user@example.com",
				null,
				"password123",
				null,
				Gender.Male,
				null,
				Set.of(1L))))
				.isInstanceOf(ConflictException.class);
	}
}
