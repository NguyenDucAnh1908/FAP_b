package com.fap.clazz.service;

import com.fap.clazz.dto.ClassTrainerItemRequest;
import com.fap.clazz.dto.ClassTrainerResponse;
import com.fap.clazz.dto.UpdateClassTrainersRequest;
import com.fap.clazz.entity.ClassTrainer;
import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.mapper.ClassTrainerMapper;
import com.fap.clazz.repository.ClassRepository;
import com.fap.clazz.repository.ClassTrainerRepository;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.security.RoleNames;
import com.fap.notification.service.NotificationService;
import com.fap.program.entity.TrainingProgram;
import com.fap.program.repository.TrainingProgramSyllabusRepository;
import com.fap.role.entity.Role;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.repository.SyllabusRepository;
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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Trainer assignments decide who may run sessions and see a class, either for the whole class or
 * for one syllabus of its training program. The list may only change while the class is being
 * planned, may only name active Trainers, and a syllabus scope must belong to the class's own
 * program; otherwise a trainer could be scoped to content the class never teaches. A replace is
 * all-or-nothing: one bad entry must leave nothing saved, audited or announced.
 */
class ClassTrainerServiceTest {

	private static final long CLASS_ID = 10L;
	private static final long PROGRAM_ID = 99L;
	private static final long SYLLABUS_ID = 5L;
	private static final String CLASS_CODE = "C01";
	private static final String CLASS_NAME = "Java Foundation";

	private final ClassRepository classRepository = mock(ClassRepository.class);
	private final ClassTrainerRepository classTrainerRepository = mock(ClassTrainerRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final SyllabusRepository syllabusRepository = mock(SyllabusRepository.class);
	private final TrainingProgramSyllabusRepository trainingProgramSyllabusRepository =
			mock(TrainingProgramSyllabusRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final NotificationService notificationService = mock(NotificationService.class);

	private final ClassTrainerService service = new ClassTrainerService(
			classRepository,
			classTrainerRepository,
			userRepository,
			syllabusRepository,
			trainingProgramSyllabusRepository,
			new ClassTrainerMapper(),
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
		lenient().doCallRealMethod().when(syllabusRepository).getOrThrow(any());
	}

	@Test
	void listRejectsUnknownClass() {
		when(classRepository.existsById(CLASS_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.list(CLASS_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Class not found")
				.extracting("code")
				.isEqualTo(NotFoundException.CODE);

		verifyNoInteractions(classTrainerRepository);
	}

	@Test
	void listReturnsClassWideAndSyllabusScopedTrainersInRepositoryOrder() {
		when(classRepository.existsById(CLASS_ID)).thenReturn(true);
		when(classTrainerRepository.findByFapClassIdOrderByIdAsc(CLASS_ID)).thenReturn(List.of(
				assignment(1L, user(7L, UserStatus.Active, Set.of(RoleNames.TRAINER)), null),
				assignment(2L, user(8L, UserStatus.Active, Set.of(RoleNames.TRAINER)), syllabus())));

		List<ClassTrainerResponse> responses = service.list(CLASS_ID);

		assertThat(responses).containsExactly(
				new ClassTrainerResponse(1L, 7L, "User 7", "user7@fap.local", null, null, null),
				new ClassTrainerResponse(2L, 8L, "User 8", "user8@fap.local", SYLLABUS_ID, "Spring Boot", "SB01"));
	}

	@Test
	void replaceAssignsClassWideTrainerWithoutConsultingTheProgram() {
		FapClass fapClass = givenClass(ClassStatus.Planning);
		givenUser(7L, UserStatus.Active, Set.of(RoleNames.TRAINER));

		List<ClassTrainerResponse> responses = service.replace(CLASS_ID, request(item(7L, null)));

		// The old rows go first so that re-listing the same trainer does not duplicate the scope.
		InOrder inOrder = inOrder(classTrainerRepository, notificationService);
		inOrder.verify(classTrainerRepository).deleteByFapClassId(CLASS_ID);
		inOrder.verify(classTrainerRepository).saveAll(any());
		inOrder.verify(notificationService).create(eq(7L), anyString(), anyString());
		ClassTrainer saved = captureSavedTrainers().getFirst();
		assertThat(saved.getFapClass()).isSameAs(fapClass);
		assertThat(saved.getUser().getId()).isEqualTo(7L);
		assertThat(saved.getSyllabus()).isNull();
		verifyNoInteractions(trainingProgramSyllabusRepository, syllabusRepository);
		verify(auditLogService).record("UPDATE_CLASS_TRAINERS", "class", CLASS_ID);
		assertThat(notifiedMessage(7L)).contains(CLASS_CODE, CLASS_NAME);
		assertThat(responses).containsExactly(
				new ClassTrainerResponse(null, 7L, "User 7", "user7@fap.local", null, null, null));
	}

	@Test
	void replaceAssignsTrainerToSyllabusOfTheClassProgram() {
		givenClass(ClassStatus.Planning);
		givenUser(7L, UserStatus.Active, Set.of(RoleNames.TRAINER));
		Syllabus syllabus = givenSyllabusInProgram();

		List<ClassTrainerResponse> responses = service.replace(CLASS_ID, request(item(7L, SYLLABUS_ID)));

		assertThat(captureSavedTrainers().getFirst().getSyllabus()).isSameAs(syllabus);
		assertThat(responses)
				.extracting(ClassTrainerResponse::userId, ClassTrainerResponse::syllabusId)
				.containsExactly(tuple(7L, SYLLABUS_ID));
		verify(auditLogService).record("UPDATE_CLASS_TRAINERS", "class", CLASS_ID);
	}

	/**
	 * Pins current behaviour: whole-class and syllabus scopes are different keys, so one trainer may
	 * hold both, and the trainer is then notified once per entry rather than once per person.
	 */
	@Test
	void replaceAllowsSameTrainerForWholeClassAndOneSyllabusAndNotifiesPerEntry() {
		givenClass(ClassStatus.Planning);
		givenUser(7L, UserStatus.Active, Set.of(RoleNames.TRAINER));
		givenSyllabusInProgram();

		service.replace(CLASS_ID, request(item(7L, null), item(7L, SYLLABUS_ID)));

		assertThat(captureSavedTrainers())
				.extracting(trainer -> trainer.getUser().getId(), trainer -> trainer.getSyllabus() == null)
				.containsExactly(
						tuple(7L, true),
						tuple(7L, false));
		verify(notificationService, times(2)).create(eq(7L), anyString(), anyString());
	}

	@Test
	void replaceAllowsDifferentTrainersOnTheSameSyllabus() {
		givenClass(ClassStatus.Planning);
		givenUser(7L, UserStatus.Active, Set.of(RoleNames.TRAINER));
		givenUser(8L, UserStatus.Active, Set.of(RoleNames.TRAINER));
		givenSyllabusInProgram();

		service.replace(CLASS_ID, request(item(7L, SYLLABUS_ID), item(8L, SYLLABUS_ID)));

		assertThat(captureSavedTrainers())
				.extracting(trainer -> trainer.getUser().getId())
				.containsExactly(7L, 8L);
		verify(notificationService).create(eq(7L), anyString(), anyString());
		verify(notificationService).create(eq(8L), anyString(), anyString());
	}

	@Test
	void replaceRejectsUnknownClass() {
		when(classRepository.findWithTrainingProgramById(CLASS_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.replace(CLASS_ID, request(item(7L, null))))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Class not found")
				.extracting("code")
				.isEqualTo(NotFoundException.CODE);

		verifyNoInteractions(classTrainerRepository, userRepository, auditLogService, notificationService);
	}

	@ParameterizedTest(name = "{0} class is not editable")
	@EnumSource(value = ClassStatus.class, names = "Planning", mode = EnumSource.Mode.EXCLUDE)
	void replaceRejectsClassThatIsNoLongerPlanning(ClassStatus status) {
		givenClass(status);

		assertThatThrownBy(() -> service.replace(CLASS_ID, request(item(7L, null))))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_NOT_EDITABLE");

		verifyNoInteractions(classTrainerRepository, userRepository, auditLogService, notificationService);
	}

	/** The duplicate check runs before the current trainers are removed, so a bad request changes nothing. */
	@ParameterizedTest(name = "{0} is a duplicate scope")
	@MethodSource("duplicateScopes")
	void replaceRejectsDuplicateTrainerScopeBeforeRemovingCurrentTrainers(List<ClassTrainerItemRequest> items) {
		givenClass(ClassStatus.Planning);

		assertThatThrownBy(() -> service.replace(CLASS_ID, new UpdateClassTrainersRequest(items)))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("DUPLICATE_CLASS_TRAINER_SCOPE");

		verifyNoInteractions(classTrainerRepository, userRepository, auditLogService, notificationService);
	}

	@Test
	void replaceRejectsUnknownUser() {
		givenClass(ClassStatus.Planning);
		when(userRepository.findWithRolesById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.replace(CLASS_ID, request(item(7L, null))))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("User not found")
				.extracting("code")
				.isEqualTo(NotFoundException.CODE);

		verify(classTrainerRepository, never()).saveAll(any());
		verifyNoInteractions(auditLogService, notificationService);
	}

	@Test
	void replaceRejectsInactiveTrainer() {
		givenClass(ClassStatus.Planning);
		givenUser(7L, UserStatus.Inactive, Set.of(RoleNames.TRAINER));

		assertThatThrownBy(() -> service.replace(CLASS_ID, request(item(7L, null))))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_TRAINER_ROLE_REQUIRED");

		verify(classTrainerRepository, never()).saveAll(any());
		verifyNoInteractions(auditLogService, notificationService);
	}

	@ParameterizedTest(name = "roles {0} are rejected")
	@MethodSource("rolesWithoutTrainer")
	void replaceRejectsUserWithoutTrainerRole(Set<String> roleNames) {
		givenClass(ClassStatus.Planning);
		givenUser(7L, UserStatus.Active, roleNames);

		assertThatThrownBy(() -> service.replace(CLASS_ID, request(item(7L, null))))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_TRAINER_ROLE_REQUIRED");

		verify(classTrainerRepository, never()).saveAll(any());
	}

	@Test
	void replaceRejectsSyllabusOutsideTheClassProgram() {
		givenClass(ClassStatus.Planning);
		givenUser(7L, UserStatus.Active, Set.of(RoleNames.TRAINER));
		when(trainingProgramSyllabusRepository.existsByIdProgramIdAndIdSyllabusId(PROGRAM_ID, SYLLABUS_ID))
				.thenReturn(false);

		assertThatThrownBy(() -> service.replace(CLASS_ID, request(item(7L, SYLLABUS_ID))))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_TRAINER_SYLLABUS_NOT_IN_PROGRAM");

		verifyNoInteractions(syllabusRepository, auditLogService, notificationService);
		verify(classTrainerRepository, never()).saveAll(any());
	}

	@Test
	void replaceRejectsUnknownSyllabusListedInTheProgram() {
		givenClass(ClassStatus.Planning);
		givenUser(7L, UserStatus.Active, Set.of(RoleNames.TRAINER));
		when(trainingProgramSyllabusRepository.existsByIdProgramIdAndIdSyllabusId(PROGRAM_ID, SYLLABUS_ID))
				.thenReturn(true);
		when(syllabusRepository.findById(SYLLABUS_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.replace(CLASS_ID, request(item(7L, SYLLABUS_ID))))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Syllabus not found")
				.extracting("code")
				.isEqualTo(NotFoundException.CODE);

		verify(classTrainerRepository, never()).saveAll(any());
		verifyNoInteractions(auditLogService, notificationService);
	}

	/** The trainer check comes first, so an ineligible user is reported as such even with a bad scope. */
	@Test
	void replaceChecksTrainerRoleBeforeSyllabusScope() {
		givenClass(ClassStatus.Planning);
		givenUser(7L, UserStatus.Inactive, Set.of(RoleNames.TRAINER));

		assertThatThrownBy(() -> service.replace(CLASS_ID, request(item(7L, SYLLABUS_ID))))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_TRAINER_ROLE_REQUIRED");

		verifyNoInteractions(trainingProgramSyllabusRepository, syllabusRepository);
	}

	/**
	 * Entries are validated while the new rows are built, after the old rows were deleted; the
	 * transaction rollback restores those. What must hold here is that a valid first entry is not
	 * saved, audited or notified when a later entry in the same request is rejected.
	 */
	@Test
	void replaceSavesAuditsAndNotifiesNothingWhenALaterEntryIsInvalid() {
		givenClass(ClassStatus.Planning);
		givenUser(7L, UserStatus.Active, Set.of(RoleNames.TRAINER));
		givenUser(8L, UserStatus.Active, Set.of(RoleNames.CLASS_ADMIN));

		assertThatThrownBy(() -> service.replace(CLASS_ID, request(item(7L, null), item(8L, null))))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_TRAINER_ROLE_REQUIRED");

		verify(classTrainerRepository, never()).saveAll(any());
		verify(notificationService, never()).create(anyLong(), anyString(), anyString());
		verifyNoInteractions(auditLogService);
	}

	static Stream<List<ClassTrainerItemRequest>> duplicateScopes() {
		return Stream.of(
				List.of(item(7L, SYLLABUS_ID), item(8L, null), item(7L, SYLLABUS_ID)),
				List.of(item(7L, null), item(7L, null)));
	}

	static Stream<Set<String>> rolesWithoutTrainer() {
		return Stream.of(
				Set.of(),
				Set.of(RoleNames.SUPER_ADMIN),
				Set.of(RoleNames.CLASS_ADMIN),
				Set.of(RoleNames.TRAINEE));
	}

	private FapClass givenClass(ClassStatus status) {
		TrainingProgram program = new TrainingProgram();
		program.setId(PROGRAM_ID);

		FapClass fapClass = new FapClass();
		fapClass.setId(CLASS_ID);
		fapClass.setClassCode(CLASS_CODE);
		fapClass.setName(CLASS_NAME);
		fapClass.setStatus(status);
		fapClass.setTrainingProgram(program);
		when(classRepository.findWithTrainingProgramById(CLASS_ID)).thenReturn(Optional.of(fapClass));
		return fapClass;
	}

	private void givenUser(long userId, UserStatus status, Set<String> roleNames) {
		when(userRepository.findWithRolesById(userId)).thenReturn(Optional.of(user(userId, status, roleNames)));
	}

	private Syllabus givenSyllabusInProgram() {
		Syllabus syllabus = syllabus();
		when(trainingProgramSyllabusRepository.existsByIdProgramIdAndIdSyllabusId(PROGRAM_ID, SYLLABUS_ID))
				.thenReturn(true);
		when(syllabusRepository.findById(SYLLABUS_ID)).thenReturn(Optional.of(syllabus));
		return syllabus;
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

	private static Syllabus syllabus() {
		Syllabus syllabus = new Syllabus();
		syllabus.setId(SYLLABUS_ID);
		syllabus.setName("Spring Boot");
		syllabus.setCode("SB01");
		return syllabus;
	}

	private static ClassTrainer assignment(long id, User user, Syllabus syllabus) {
		ClassTrainer classTrainer = new ClassTrainer();
		classTrainer.setId(id);
		classTrainer.setUser(user);
		classTrainer.setSyllabus(syllabus);
		return classTrainer;
	}

	private static ClassTrainerItemRequest item(Long userId, Long syllabusId) {
		return new ClassTrainerItemRequest(userId, syllabusId);
	}

	private static UpdateClassTrainersRequest request(ClassTrainerItemRequest... items) {
		return new UpdateClassTrainersRequest(List.of(items));
	}

	@SuppressWarnings("unchecked")
	private List<ClassTrainer> captureSavedTrainers() {
		ArgumentCaptor<Iterable<ClassTrainer>> captor = ArgumentCaptor.forClass(Iterable.class);
		verify(classTrainerRepository).saveAll(captor.capture());
		List<ClassTrainer> saved = new ArrayList<>();
		captor.getValue().forEach(saved::add);
		return saved;
	}

	private String notifiedMessage(long userId) {
		ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
		verify(notificationService).create(eq(userId), anyString(), message.capture());
		return message.getValue();
	}
}
