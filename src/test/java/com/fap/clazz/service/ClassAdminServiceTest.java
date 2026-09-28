package com.fap.clazz.service;

import com.fap.clazz.dto.ClassAdminResponse;
import com.fap.clazz.dto.UpdateClassAdminsRequest;
import com.fap.clazz.entity.ClassAdmin;
import com.fap.clazz.entity.ClassAdminId;
import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.mapper.ClassAdminMapper;
import com.fap.clazz.repository.ClassAdminRepository;
import com.fap.clazz.repository.ClassRepository;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.security.RoleNames;
import com.fap.notification.service.NotificationService;
import com.fap.role.entity.Role;
import com.fap.user.entity.User;
import com.fap.user.enums.UserStatus;
import com.fap.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Class admins run a class day to day and gain management rights over it through this list, so
 * the list may only change while the class is still being planned and may only name active users
 * who hold the Class Admin role. A replace is all-or-nothing: one bad user id must leave nothing
 * saved, audited or announced, otherwise users would be told about an assignment that was rolled
 * back.
 */
class ClassAdminServiceTest {

	private static final long CLASS_ID = 10L;
	private static final String CLASS_CODE = "C01";
	private static final String CLASS_NAME = "Java Foundation";

	private final ClassRepository classRepository = mock(ClassRepository.class);
	private final ClassAdminRepository classAdminRepository = mock(ClassAdminRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final NotificationService notificationService = mock(NotificationService.class);

	private final ClassAdminService service = new ClassAdminService(
			classRepository,
			classAdminRepository,
			userRepository,
			new ClassAdminMapper(),
			auditLogService,
			notificationService);

	/**
	 * The finders are the single stubbed source of truth, whether the service calls them directly or
	 * through the repositories' {@code get...OrThrow} lookup defaults.
	 */
	@BeforeEach
	void lookupDefaultsDelegateToStubbedFinders() {
		lenient().doCallRealMethod().when(classRepository).getWithTrainingProgramOrThrow(any());
		lenient().doCallRealMethod().when(userRepository).getWithRolesOrThrow(any());
	}

	@Test
	void listRejectsUnknownClass() {
		when(classRepository.existsById(CLASS_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.list(CLASS_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Class not found")
				.extracting("code")
				.isEqualTo(NotFoundException.CODE);

		verifyNoInteractions(classAdminRepository);
	}

	@Test
	void listReturnsAdminsInRepositoryOrder() {
		when(classRepository.existsById(CLASS_ID)).thenReturn(true);
		when(classAdminRepository.findByFapClassIdOrderByUserFullNameAsc(CLASS_ID)).thenReturn(List.of(
				assignment(user(8L, UserStatus.Active, Set.of(RoleNames.CLASS_ADMIN))),
				assignment(user(7L, UserStatus.Active, Set.of(RoleNames.CLASS_ADMIN)))));

		List<ClassAdminResponse> responses = service.list(CLASS_ID);

		assertThat(responses).containsExactly(
				new ClassAdminResponse(8L, "User 8", "user8@fap.local"),
				new ClassAdminResponse(7L, "User 7", "user7@fap.local"));
	}

	@Test
	void replaceSwapsAdminsThenAuditsAndNotifiesEachNewAdmin() {
		FapClass fapClass = givenClass(ClassStatus.Planning);
		givenUser(7L, UserStatus.Active, Set.of(RoleNames.CLASS_ADMIN));
		givenUser(8L, UserStatus.Active, Set.of(RoleNames.CLASS_ADMIN));

		List<ClassAdminResponse> responses = service.replace(CLASS_ID, request(7L, 8L));

		// The old rows go first: the composite key (class, user) would collide if a user stays on.
		InOrder inOrder = inOrder(classAdminRepository, notificationService);
		inOrder.verify(classAdminRepository).deleteByFapClassId(CLASS_ID);
		inOrder.verify(classAdminRepository).saveAll(any());
		inOrder.verify(notificationService).create(eq(7L), anyString(), anyString());
		List<ClassAdmin> saved = captureSavedAdmins();
		assertThat(saved)
				.extracting(ClassAdmin::getId)
				.containsExactly(new ClassAdminId(CLASS_ID, 7L), new ClassAdminId(CLASS_ID, 8L));
		assertThat(saved).allSatisfy(admin -> assertThat(admin.getFapClass()).isSameAs(fapClass));
		assertThat(saved)
				.extracting(admin -> admin.getUser().getId())
				.containsExactly(7L, 8L);
		verify(auditLogService).record("UPDATE_CLASS_ADMINS", "class", CLASS_ID);
		assertThat(notifiedMessages(7L, 8L)).allSatisfy(message -> assertThat(message).contains(CLASS_CODE, CLASS_NAME));
		assertThat(responses).containsExactly(
				new ClassAdminResponse(7L, "User 7", "user7@fap.local"),
				new ClassAdminResponse(8L, "User 8", "user8@fap.local"));
	}

	@Test
	void replaceAcceptsActiveUserHoldingClassAdminAmongOtherRoles() {
		givenClass(ClassStatus.Planning);
		givenUser(7L, UserStatus.Active, Set.of(RoleNames.TRAINER, RoleNames.CLASS_ADMIN));

		List<ClassAdminResponse> responses = service.replace(CLASS_ID, request(7L));

		assertThat(responses).extracting(ClassAdminResponse::userId).containsExactly(7L);
		assertThat(captureSavedAdmins()).hasSize(1);
	}

	@Test
	void replaceRejectsUnknownClass() {
		when(classRepository.findWithTrainingProgramById(CLASS_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.replace(CLASS_ID, request(7L)))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Class not found")
				.extracting("code")
				.isEqualTo(NotFoundException.CODE);

		verifyNoInteractions(classAdminRepository, userRepository, auditLogService, notificationService);
	}

	@ParameterizedTest(name = "{0} class is not editable")
	@EnumSource(value = ClassStatus.class, names = "Planning", mode = EnumSource.Mode.EXCLUDE)
	void replaceRejectsClassThatIsNoLongerPlanning(ClassStatus status) {
		givenClass(status);

		assertThatThrownBy(() -> service.replace(CLASS_ID, request(7L)))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_NOT_EDITABLE");

		verifyNoInteractions(classAdminRepository, userRepository, auditLogService, notificationService);
	}

	/** The duplicate check runs before the current admins are removed, so a bad request changes nothing. */
	@Test
	void replaceRejectsDuplicateUserBeforeRemovingCurrentAdmins() {
		givenClass(ClassStatus.Planning);

		assertThatThrownBy(() -> service.replace(CLASS_ID, request(7L, 8L, 7L)))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("DUPLICATE_CLASS_ADMIN");

		verifyNoInteractions(classAdminRepository, userRepository, auditLogService, notificationService);
	}

	@Test
	void replaceRejectsUnknownUser() {
		givenClass(ClassStatus.Planning);
		when(userRepository.findWithRolesById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.replace(CLASS_ID, request(7L)))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("User not found")
				.extracting("code")
				.isEqualTo(NotFoundException.CODE);

		verify(classAdminRepository, never()).saveAll(any());
		verifyNoInteractions(auditLogService, notificationService);
	}

	@Test
	void replaceRejectsInactiveClassAdmin() {
		givenClass(ClassStatus.Planning);
		givenUser(7L, UserStatus.Inactive, Set.of(RoleNames.CLASS_ADMIN));

		assertThatThrownBy(() -> service.replace(CLASS_ID, request(7L)))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_ADMIN_ROLE_REQUIRED");

		verify(classAdminRepository, never()).saveAll(any());
		verifyNoInteractions(auditLogService, notificationService);
	}

	/** Holding a more powerful role is not enough: only the Class Admin role makes a user assignable. */
	@ParameterizedTest(name = "roles {0} are rejected")
	@MethodSource("rolesWithoutClassAdmin")
	void replaceRejectsUserWithoutClassAdminRole(Set<String> roleNames) {
		givenClass(ClassStatus.Planning);
		givenUser(7L, UserStatus.Active, roleNames);

		assertThatThrownBy(() -> service.replace(CLASS_ID, request(7L)))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_ADMIN_ROLE_REQUIRED");

		verify(classAdminRepository, never()).saveAll(any());
	}

	/**
	 * Users are validated while the new rows are built, after the old rows were deleted; the
	 * transaction rollback restores those. What must hold here is that a valid first user is not
	 * saved, audited or notified when a later user in the same request is rejected.
	 */
	@Test
	void replaceSavesAuditsAndNotifiesNothingWhenALaterUserIsInvalid() {
		givenClass(ClassStatus.Planning);
		givenUser(7L, UserStatus.Active, Set.of(RoleNames.CLASS_ADMIN));
		givenUser(8L, UserStatus.Active, Set.of(RoleNames.TRAINER));

		assertThatThrownBy(() -> service.replace(CLASS_ID, request(7L, 8L)))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_ADMIN_ROLE_REQUIRED");

		verify(classAdminRepository, never()).saveAll(any());
		verifyNoInteractions(auditLogService, notificationService);
	}

	static Stream<Set<String>> rolesWithoutClassAdmin() {
		return Stream.of(
				Set.of(),
				Set.of(RoleNames.SUPER_ADMIN),
				Set.of(RoleNames.TRAINER),
				Set.of(RoleNames.TRAINEE, RoleNames.TRAINER));
	}

	private FapClass givenClass(ClassStatus status) {
		FapClass fapClass = new FapClass();
		fapClass.setId(CLASS_ID);
		fapClass.setClassCode(CLASS_CODE);
		fapClass.setName(CLASS_NAME);
		fapClass.setStatus(status);
		when(classRepository.findWithTrainingProgramById(CLASS_ID)).thenReturn(Optional.of(fapClass));
		return fapClass;
	}

	private void givenUser(long userId, UserStatus status, Set<String> roleNames) {
		when(userRepository.findWithRolesById(userId)).thenReturn(Optional.of(user(userId, status, roleNames)));
	}

	private static User user(long userId, UserStatus status, Set<String> roleNames) {
		User user = new User();
		user.setId(userId);
		user.setFullName("User " + userId);
		user.setEmail("user" + userId + "@fap.local");
		user.setStatus(status);
		for (String roleName : roleNames) {
			Role role = new Role();
			role.setName(roleName);
			user.getRoles().add(role);
		}
		return user;
	}

	private static ClassAdmin assignment(User user) {
		ClassAdmin classAdmin = new ClassAdmin();
		classAdmin.setId(new ClassAdminId(CLASS_ID, user.getId()));
		classAdmin.setUser(user);
		return classAdmin;
	}

	private static UpdateClassAdminsRequest request(Long... userIds) {
		return new UpdateClassAdminsRequest(List.of(userIds));
	}

	@SuppressWarnings("unchecked")
	private List<ClassAdmin> captureSavedAdmins() {
		ArgumentCaptor<Iterable<ClassAdmin>> captor = ArgumentCaptor.forClass(Iterable.class);
		verify(classAdminRepository).saveAll(captor.capture());
		List<ClassAdmin> saved = new ArrayList<>();
		captor.getValue().forEach(saved::add);
		return saved;
	}

	/** Exactly one notification per listed user, in order; returns the message bodies. */
	private List<String> notifiedMessages(Long... userIds) {
		ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
		for (Long userId : userIds) {
			verify(notificationService).create(eq(userId), anyString(), messages.capture());
		}
		verifyNoMoreInteractions(notificationService);
		return messages.getAllValues();
	}
}
