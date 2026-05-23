package com.fap.user.service;

import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.role.entity.Role;
import com.fap.role.repository.RoleRepository;
import com.fap.user.dto.CreateUserRequest;
import com.fap.user.dto.UpdateUserRequest;
import com.fap.user.dto.UserResponse;
import com.fap.user.entity.User;
import com.fap.user.enums.UserStatus;
import com.fap.user.mapper.UserMapper;
import com.fap.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserMapper userMapper;

	public UserService(
			UserRepository userRepository,
			RoleRepository roleRepository,
			PasswordEncoder passwordEncoder,
			UserMapper userMapper) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.userMapper = userMapper;
	}

	@Transactional(readOnly = true)
	public Page<UserResponse> list(UserStatus status, int page, int limit) {
		PageRequest pageRequest = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<User> users = status == null
				? userRepository.findAll(pageRequest)
				: userRepository.findByStatus(status, pageRequest);
		return users.map(userMapper::toResponse);
	}

	@Transactional
	public UserResponse create(CreateUserRequest request) {
		if (userRepository.existsByEmailIgnoreCase(request.email())) {
			throw new ConflictException("Email already exists");
		}
		LocalDateTime now = LocalDateTime.now();
		User user = new User();
		user.setFullName(request.fullName());
		user.setEmail(request.email().trim().toLowerCase());
		user.setPhone(request.phone());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setDateOfBirth(request.dateOfBirth());
		user.setGender(request.gender());
		user.setAvatarUrl(request.avatarUrl());
		user.setStatus(UserStatus.Active);
		user.setCreatedAt(now);
		user.setUpdatedAt(now);
		user.setRoles(resolveRoles(request.roleIds()));
		return userMapper.toResponse(userRepository.save(user));
	}

	@Transactional(readOnly = true)
	public UserResponse get(Long id) {
		return userMapper.toResponse(findUser(id));
	}

	@Transactional
	public UserResponse update(Long id, UpdateUserRequest request) {
		User user = findUser(id);
		userRepository.findByEmailIgnoreCase(request.email())
				.filter(existing -> !existing.getId().equals(id))
				.ifPresent(existing -> {
					throw new ConflictException("Email already exists");
				});
		user.setFullName(request.fullName());
		user.setEmail(request.email().trim().toLowerCase());
		user.setPhone(request.phone());
		user.setDateOfBirth(request.dateOfBirth());
		user.setGender(request.gender());
		user.setAvatarUrl(request.avatarUrl());
		user.setRoles(resolveRoles(request.roleIds()));
		user.setUpdatedAt(LocalDateTime.now());
		return userMapper.toResponse(user);
	}

	@Transactional
	public UserResponse updateStatus(Long id, UserStatus status) {
		User user = findUser(id);
		user.setStatus(status);
		user.setUpdatedAt(LocalDateTime.now());
		return userMapper.toResponse(user);
	}

	private User findUser(Long id) {
		return userRepository.findWithRolesById(id)
				.orElseThrow(() -> new NotFoundException("User not found"));
	}

	private Set<Role> resolveRoles(Set<Long> roleIds) {
		Set<Role> roles = roleRepository.findByIdIn(roleIds);
		if (roles.size() != roleIds.size()) {
			throw new NotFoundException("One or more roles were not found");
		}
		return roles;
	}
}
