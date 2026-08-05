package com.fap.user.service;

import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.audit.AuditLogService;
import com.fap.common.api.PageRequestFactory;
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
import java.util.Locale;
import java.util.Set;

@Service
public class UserService {

	private static final String SUPER_ADMIN_ROLE = "Super Admin";

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserMapper userMapper;
	private final AuditLogService auditLogService;

	public UserService(
			UserRepository userRepository,
			RoleRepository roleRepository,
			PasswordEncoder passwordEncoder,
			UserMapper userMapper,
			AuditLogService auditLogService) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.userMapper = userMapper;
		this.auditLogService = auditLogService;
	}

	@Transactional(readOnly = true)
	public Page<UserResponse> list(
			UserStatus status,
			String keyword,
			String email,
			String fullName,
			Long roleId,
			String roleName,
			int page,
			int limit) {
		return list(status, keyword, email, fullName, roleId, roleName, page, limit, null, null);
	}

	@Transactional(readOnly = true)
	public Page<UserResponse> list(
			UserStatus status,
			String keyword,
			String email,
			String fullName,
			Long roleId,
			String roleName,
			int page,
			int limit,
			String sortBy,
			String order) {
		PageRequest pageRequest = PageRequestFactory.create(
				page,
				limit,
				sortBy,
				order,
				Sort.by(Sort.Direction.DESC, "createdAt"),
				"id", "createdAt", "fullName", "email", "status");
		Page<User> users = userRepository.search(
				status,
				normalizeLikeFilter(keyword),
				normalizeLikeFilter(email),
				normalizeLikeFilter(fullName),
				roleId,
				normalizeExactFilter(roleName),
				pageRequest);
		return users.map(userMapper::toResponse);
	}

	@Transactional
	public UserResponse create(CreateUserRequest request, Set<String> currentUserRoles) {
		if (userRepository.existsByEmailIgnoreCase(request.email())) {
			throw new ConflictException("Email already exists");
		}
		Set<Role> roles = resolveRoles(request.roleIds());
		validateSuperAdminRoleChange(false, hasSuperAdminRole(roles), currentUserRoles);
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
		user.setRoles(roles);
		User saved = userRepository.save(user);
		auditLogService.record("CREATE_USER", "user", saved.getId());
		return userMapper.toResponse(saved);
	}

	@Transactional(readOnly = true)
	public UserResponse get(Long id) {
		return userMapper.toResponse(findUser(id));
	}

	@Transactional
	public UserResponse update(Long id, UpdateUserRequest request, Set<String> currentUserRoles) {
		User user = findUser(id);
		userRepository.findByEmailIgnoreCase(request.email())
				.filter(existing -> !existing.getId().equals(id))
				.ifPresent(existing -> {
					throw new ConflictException("Email already exists");
				});
		Set<Role> roles = resolveRoles(request.roleIds());
		boolean currentlySuperAdmin = isSuperAdmin(user);
		boolean requestedSuperAdmin = hasSuperAdminRole(roles);
		validateSuperAdminRoleChange(currentlySuperAdmin, requestedSuperAdmin, currentUserRoles);
		if (currentlySuperAdmin
				&& !requestedSuperAdmin
				&& user.getStatus() == UserStatus.Active
				&& userRepository.countByRoleNameAndStatus(SUPER_ADMIN_ROLE, UserStatus.Active) <= 1) {
			throw new ConflictException("CANNOT_REMOVE_LAST_SUPER_ADMIN_ROLE", "Cannot remove Super Admin role from the last active Super Admin");
		}
		user.setFullName(request.fullName());
		user.setEmail(request.email().trim().toLowerCase());
		user.setPhone(request.phone());
		user.setDateOfBirth(request.dateOfBirth());
		user.setGender(request.gender());
		user.setAvatarUrl(request.avatarUrl());
		user.setRoles(roles);
		user.setUpdatedAt(LocalDateTime.now());
		auditLogService.record("UPDATE_USER", "user", user.getId());
		return userMapper.toResponse(user);
	}

	@Transactional
	public UserResponse updateStatus(Long id, UserStatus status, Long currentUserId) {
		User user = findUser(id);
		if (status == UserStatus.Inactive) {
			if (user.getId().equals(currentUserId)) {
				throw new ConflictException("CANNOT_DEACTIVATE_SELF", "Cannot deactivate your own account");
			}
			if (isSuperAdmin(user) && userRepository.countByRoleNameAndStatus(SUPER_ADMIN_ROLE, UserStatus.Active) <= 1) {
				throw new ConflictException("CANNOT_DEACTIVATE_LAST_SUPER_ADMIN", "Cannot deactivate the last active Super Admin");
			}
		}
		user.setStatus(status);
		user.setUpdatedAt(LocalDateTime.now());
		auditLogService.record("UPDATE_USER_STATUS:" + status.name(), "user", user.getId());
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

	private boolean isSuperAdmin(User user) {
		return hasSuperAdminRole(user.getRoles());
	}

	private boolean hasSuperAdminRole(Set<Role> roles) {
		return roles.stream()
				.anyMatch(role -> SUPER_ADMIN_ROLE.equals(role.getName()));
	}

	private void validateSuperAdminRoleChange(
			boolean currentHasSuperAdminRole,
			boolean requestedHasSuperAdminRole,
			Set<String> currentUserRoles) {
		if (currentHasSuperAdminRole != requestedHasSuperAdminRole && !currentUserRoles.contains(SUPER_ADMIN_ROLE)) {
			throw new ConflictException("CANNOT_MANAGE_SUPER_ADMIN_ROLE", "Only Super Admin can manage Super Admin role");
		}
	}

	private String normalizeLikeFilter(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String normalizeExactFilter(String value) {
		return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
	}
}
