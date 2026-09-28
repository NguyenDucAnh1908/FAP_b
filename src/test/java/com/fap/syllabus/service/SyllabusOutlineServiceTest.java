package com.fap.syllabus.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.syllabus.dto.CreateSyllabusDayRequest;
import com.fap.syllabus.dto.CreateSyllabusTopicRequest;
import com.fap.syllabus.dto.CreateSyllabusUnitRequest;
import com.fap.syllabus.dto.SyllabusDayResponse;
import com.fap.syllabus.dto.SyllabusTopicResponse;
import com.fap.syllabus.dto.SyllabusUnitResponse;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.entity.SyllabusDay;
import com.fap.syllabus.entity.SyllabusTopic;
import com.fap.syllabus.entity.SyllabusUnit;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.enums.SyllabusTopicStatus;
import com.fap.syllabus.mapper.MaterialFileMapper;
import com.fap.syllabus.mapper.SyllabusOutlineMapper;
import com.fap.syllabus.repository.SyllabusDayRepository;
import com.fap.syllabus.repository.SyllabusRepository;
import com.fap.syllabus.repository.SyllabusTopicRepository;
import com.fap.syllabus.repository.SyllabusUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Guards outline editing. A published (Active or Inactive) syllabus is frozen because classes may
 * already be teaching it, and every day, unit and topic is looked up through its syllabus id, so a
 * caller allowed to edit one syllabus cannot reach another syllabus's outline by guessing child ids.
 */
class SyllabusOutlineServiceTest {

	private static final long SYLLABUS_ID = 10L;
	private static final long OTHER_SYLLABUS_ID = 11L;
	private static final long DAY_ID = 20L;
	private static final long UNIT_ID = 30L;
	private static final long TOPIC_ID = 40L;

	private static final String DAY_NOT_FOUND = "Syllabus day not found";
	private static final String UNIT_NOT_FOUND = "Syllabus unit not found";
	private static final String TOPIC_NOT_FOUND = "Syllabus topic not found";

	private static final Named<OutlineMutation> CREATE_DAY =
			mutation("createDay", outline -> outline.createDay(SYLLABUS_ID, dayRequest()));
	private static final Named<OutlineMutation> UPDATE_DAY =
			mutation("updateDay", outline -> outline.updateDay(SYLLABUS_ID, DAY_ID, dayRequest()));
	private static final Named<OutlineMutation> DELETE_DAY =
			mutation("deleteDay", outline -> outline.deleteDay(SYLLABUS_ID, DAY_ID));
	private static final Named<OutlineMutation> CREATE_UNIT =
			mutation("createUnit", outline -> outline.createUnit(SYLLABUS_ID, DAY_ID, unitRequest()));
	private static final Named<OutlineMutation> UPDATE_UNIT =
			mutation("updateUnit", outline -> outline.updateUnit(SYLLABUS_ID, UNIT_ID, unitRequest()));
	private static final Named<OutlineMutation> DELETE_UNIT =
			mutation("deleteUnit", outline -> outline.deleteUnit(SYLLABUS_ID, UNIT_ID));
	private static final Named<OutlineMutation> CREATE_TOPIC =
			mutation("createTopic", outline -> outline.createTopic(SYLLABUS_ID, UNIT_ID, topicRequest()));
	private static final Named<OutlineMutation> UPDATE_TOPIC =
			mutation("updateTopic", outline -> outline.updateTopic(SYLLABUS_ID, TOPIC_ID, topicRequest()));
	private static final Named<OutlineMutation> DELETE_TOPIC =
			mutation("deleteTopic", outline -> outline.deleteTopic(SYLLABUS_ID, TOPIC_ID));

	private final SyllabusRepository syllabusRepository = mock(SyllabusRepository.class);
	private final SyllabusDayRepository dayRepository = mock(SyllabusDayRepository.class);
	private final SyllabusUnitRepository unitRepository = mock(SyllabusUnitRepository.class);
	private final SyllabusTopicRepository topicRepository = mock(SyllabusTopicRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);

	private final SyllabusOutlineService service = new SyllabusOutlineService(
			syllabusRepository,
			dayRepository,
			unitRepository,
			topicRepository,
			new SyllabusOutlineMapper(new MaterialFileMapper()),
			auditLogService);

	/** A service call that changes the outline, run against the service under test. */
	@FunctionalInterface
	interface OutlineMutation {
		void applyTo(SyllabusOutlineService outline);
	}

	@BeforeEach
	void useRealLookupDefaults() {
		lenient().doCallRealMethod().when(syllabusRepository).getOrThrow(any());
		lenient().doCallRealMethod().when(dayRepository).getByIdAndSyllabusIdOrThrow(any(), any());
		lenient().doCallRealMethod().when(unitRepository).getByIdAndDaySyllabusIdOrThrow(any(), any());
		lenient().doCallRealMethod().when(topicRepository).getByIdAndUnitDaySyllabusIdOrThrow(any(), any());
	}

	@BeforeEach
	void saveAssignsTheGeneratedId() {
		lenient().when(dayRepository.save(any(SyllabusDay.class))).thenAnswer(invocation -> {
			SyllabusDay day = invocation.getArgument(0);
			day.setId(DAY_ID);
			return day;
		});
		lenient().when(unitRepository.save(any(SyllabusUnit.class))).thenAnswer(invocation -> {
			SyllabusUnit unit = invocation.getArgument(0);
			unit.setId(UNIT_ID);
			return unit;
		});
		lenient().when(topicRepository.save(any(SyllabusTopic.class))).thenAnswer(invocation -> {
			SyllabusTopic topic = invocation.getArgument(0);
			topic.setId(TOPIC_ID);
			return topic;
		});
	}

	static Stream<Named<OutlineMutation>> mutations() {
		return Stream.of(
				CREATE_DAY, UPDATE_DAY, DELETE_DAY,
				CREATE_UNIT, UPDATE_UNIT, DELETE_UNIT,
				CREATE_TOPIC, UPDATE_TOPIC, DELETE_TOPIC);
	}

	static Stream<Arguments> mutationsOnPublishedSyllabus() {
		return Stream.of(SyllabusStatus.Active, SyllabusStatus.Inactive)
				.flatMap(status -> mutations().map(mutation -> Arguments.of(status, mutation)));
	}

	/** Every mutation that addresses an existing child, with the scoped lookup that must reject it. */
	static Stream<Arguments> mutationsAddressingAChild() {
		return Stream.of(
				Arguments.of(UPDATE_DAY, DAY_NOT_FOUND),
				Arguments.of(DELETE_DAY, DAY_NOT_FOUND),
				Arguments.of(CREATE_UNIT, DAY_NOT_FOUND),
				Arguments.of(UPDATE_UNIT, UNIT_NOT_FOUND),
				Arguments.of(DELETE_UNIT, UNIT_NOT_FOUND),
				Arguments.of(CREATE_TOPIC, UNIT_NOT_FOUND),
				Arguments.of(UPDATE_TOPIC, TOPIC_NOT_FOUND),
				Arguments.of(DELETE_TOPIC, TOPIC_NOT_FOUND));
	}

	@Test
	void getOutlineRejectsUnknownSyllabus() {
		when(syllabusRepository.existsById(SYLLABUS_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.getOutline(SYLLABUS_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Syllabus not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		verifyNoInteractions(dayRepository);
	}

	@Test
	void getOutlineReturnsDaysInRepositoryOrderWithTheirUnitsAndTopics() {
		SyllabusDay firstDay = newDay(21L, 1, 1);
		SyllabusUnit unit = newUnit(31L, firstDay, "Basics", 1);
		firstDay.getUnits().add(unit);
		unit.getTopics().add(newTopic(41L, unit, "Variables", "H1SD"));
		SyllabusDay secondDay = newDay(22L, 2, 2);
		when(syllabusRepository.existsById(SYLLABUS_ID)).thenReturn(true);
		when(dayRepository.findBySyllabusIdOrderBySortOrderAsc(SYLLABUS_ID)).thenReturn(List.of(firstDay, secondDay));

		List<SyllabusDayResponse> outline = service.getOutline(SYLLABUS_ID);

		assertThat(outline).extracting(SyllabusDayResponse::id).containsExactly(21L, 22L);
		assertThat(outline).extracting(SyllabusDayResponse::dayNumber).containsExactly(1, 2);
		assertThat(outline.get(0).units()).singleElement().satisfies(unitResponse -> {
			assertThat(unitResponse.name()).isEqualTo("Basics");
			assertThat(unitResponse.topics()).extracting(SyllabusTopicResponse::name).containsExactly("Variables");
		});
		assertThat(outline.get(1).units()).isEmpty();
	}

	@ParameterizedTest(name = "{1} is rejected on an {0} syllabus")
	@MethodSource("mutationsOnPublishedSyllabus")
	void mutationsRejectPublishedSyllabus(SyllabusStatus status, OutlineMutation mutation) {
		givenSyllabus(status);

		assertThatThrownBy(() -> mutation.applyTo(service))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("SYLLABUS_NOT_EDITABLE");

		// Editability is decided before any child is looked up, so nothing is read, written or audited.
		verifyNoInteractions(dayRepository, unitRepository, topicRepository, auditLogService);
	}

	@ParameterizedTest(name = "{0} on an unknown syllabus is not found")
	@MethodSource("mutations")
	void mutationsRejectUnknownSyllabus(OutlineMutation mutation) {
		when(syllabusRepository.findById(SYLLABUS_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> mutation.applyTo(service))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Syllabus not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		verifyNoInteractions(dayRepository, unitRepository, topicRepository, auditLogService);
	}

	@ParameterizedTest(name = "{0} cannot reach a child of another syllabus")
	@MethodSource("mutationsAddressingAChild")
	void mutationsRejectChildOfAnotherSyllabus(OutlineMutation mutation, String expectedMessage) {
		givenSyllabus(SyllabusStatus.Drafting);
		givenOutlineOwnedByAnotherSyllabus();

		assertThatThrownBy(() -> mutation.applyTo(service))
				.isInstanceOf(NotFoundException.class)
				.hasMessage(expectedMessage)
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		verifyNothingWritten();
	}

	@ParameterizedTest(name = "a day can be added to a {0} syllabus")
	@EnumSource(value = SyllabusStatus.class, names = {"Drafting", "Pending"})
	void createDayAddsTheDayToAnEditableSyllabus(SyllabusStatus status) {
		Syllabus syllabus = givenSyllabus(status);

		SyllabusDayResponse response = service.createDay(SYLLABUS_ID, new CreateSyllabusDayRequest(3, 2));

		ArgumentCaptor<SyllabusDay> saved = ArgumentCaptor.forClass(SyllabusDay.class);
		verify(dayRepository).save(saved.capture());
		assertThat(saved.getValue().getSyllabus()).isSameAs(syllabus);
		assertThat(saved.getValue().getDayNumber()).isEqualTo(3);
		assertThat(saved.getValue().getSortOrder()).isEqualTo(2);
		assertThat(response).isEqualTo(new SyllabusDayResponse(DAY_ID, 3, 2, List.of()));
		verify(auditLogService).record("CREATE_SYLLABUS_DAY", "syllabus", SYLLABUS_ID);
	}

	@Test
	void updateDayChangesNumberAndOrder() {
		SyllabusDay day = givenDay(givenSyllabus(SyllabusStatus.Pending));

		SyllabusDayResponse response = service.updateDay(SYLLABUS_ID, DAY_ID, new CreateSyllabusDayRequest(5, 4));

		assertThat(day.getDayNumber()).isEqualTo(5);
		assertThat(day.getSortOrder()).isEqualTo(4);
		assertThat(response).isEqualTo(new SyllabusDayResponse(DAY_ID, 5, 4, List.of()));
		verify(auditLogService).record("UPDATE_SYLLABUS_DAY", "syllabus", SYLLABUS_ID);
	}

	@Test
	void deleteDayDeletesTheDayAndAudits() {
		SyllabusDay day = givenDay(givenSyllabus(SyllabusStatus.Drafting));

		service.deleteDay(SYLLABUS_ID, DAY_ID);

		verify(dayRepository).delete(day);
		verify(auditLogService).record("DELETE_SYLLABUS_DAY", "syllabus", SYLLABUS_ID);
	}

	@Test
	void createUnitAttachesTheUnitToTheDay() {
		SyllabusDay day = givenDay(givenSyllabus(SyllabusStatus.Drafting));

		SyllabusUnitResponse response = service.createUnit(
				SYLLABUS_ID, DAY_ID, new CreateSyllabusUnitRequest("OOP", 2));

		ArgumentCaptor<SyllabusUnit> saved = ArgumentCaptor.forClass(SyllabusUnit.class);
		verify(unitRepository).save(saved.capture());
		assertThat(saved.getValue().getDay()).isSameAs(day);
		assertThat(saved.getValue().getName()).isEqualTo("OOP");
		assertThat(saved.getValue().getSortOrder()).isEqualTo(2);
		assertThat(response).isEqualTo(new SyllabusUnitResponse(UNIT_ID, "OOP", 2, List.of()));
		verify(auditLogService).record("CREATE_SYLLABUS_UNIT", "syllabus", SYLLABUS_ID);
	}

	@Test
	void updateUnitChangesNameAndOrder() {
		SyllabusUnit unit = givenUnit(givenSyllabus(SyllabusStatus.Drafting));

		SyllabusUnitResponse response = service.updateUnit(
				SYLLABUS_ID, UNIT_ID, new CreateSyllabusUnitRequest("Collections", 3));

		assertThat(unit.getName()).isEqualTo("Collections");
		assertThat(unit.getSortOrder()).isEqualTo(3);
		assertThat(response).isEqualTo(new SyllabusUnitResponse(UNIT_ID, "Collections", 3, List.of()));
		verify(auditLogService).record("UPDATE_SYLLABUS_UNIT", "syllabus", SYLLABUS_ID);
	}

	@Test
	void deleteUnitDeletesTheUnitAndAudits() {
		SyllabusUnit unit = givenUnit(givenSyllabus(SyllabusStatus.Pending));

		service.deleteUnit(SYLLABUS_ID, UNIT_ID);

		verify(unitRepository).delete(unit);
		verify(auditLogService).record("DELETE_SYLLABUS_UNIT", "syllabus", SYLLABUS_ID);
	}

	@Test
	void createTopicCopiesEveryRequestField() {
		SyllabusUnit unit = givenUnit(givenSyllabus(SyllabusStatus.Drafting));

		// Values differ from the entity defaults (online, 30 minutes, Active) so each copy is observable.
		// The output standard is not checked against the syllabus's selected standards here; that only
		// happens when the syllabus is submitted.
		SyllabusTopicResponse response = service.createTopic(SYLLABUS_ID, UNIT_ID, new CreateSyllabusTopicRequest(
				"Generics", "K6SD", false, 45, SyllabusTopicStatus.Inactive, 2));

		ArgumentCaptor<SyllabusTopic> saved = ArgumentCaptor.forClass(SyllabusTopic.class);
		verify(topicRepository).save(saved.capture());
		SyllabusTopic topic = saved.getValue();
		assertThat(topic.getUnit()).isSameAs(unit);
		assertThat(topic.getName()).isEqualTo("Generics");
		assertThat(topic.getOutputStandard()).isEqualTo("K6SD");
		assertThat(topic.isOnline()).isFalse();
		assertThat(topic.getDurationMinutes()).isEqualTo(45);
		assertThat(topic.getStatus()).isEqualTo(SyllabusTopicStatus.Inactive);
		assertThat(topic.getSortOrder()).isEqualTo(2);
		assertThat(response).isEqualTo(new SyllabusTopicResponse(
				TOPIC_ID, "Generics", "K6SD", false, 45, SyllabusTopicStatus.Inactive, 2));
		verify(auditLogService).record("CREATE_SYLLABUS_TOPIC", "syllabus", SYLLABUS_ID);
	}

	@Test
	void updateTopicReplacesEveryField() {
		SyllabusTopic topic = givenTopic(givenSyllabus(SyllabusStatus.Pending));

		SyllabusTopicResponse response = service.updateTopic(SYLLABUS_ID, TOPIC_ID, new CreateSyllabusTopicRequest(
				"Streams", "C3SD", false, 90, SyllabusTopicStatus.Inactive, 7));

		assertThat(topic.getName()).isEqualTo("Streams");
		assertThat(topic.getOutputStandard()).isEqualTo("C3SD");
		assertThat(topic.isOnline()).isFalse();
		assertThat(topic.getDurationMinutes()).isEqualTo(90);
		assertThat(topic.getStatus()).isEqualTo(SyllabusTopicStatus.Inactive);
		assertThat(topic.getSortOrder()).isEqualTo(7);
		assertThat(response).isEqualTo(new SyllabusTopicResponse(
				TOPIC_ID, "Streams", "C3SD", false, 90, SyllabusTopicStatus.Inactive, 7));
		verify(auditLogService).record("UPDATE_SYLLABUS_TOPIC", "syllabus", SYLLABUS_ID);
	}

	@Test
	void deleteTopicDeletesTheTopicAndAudits() {
		SyllabusTopic topic = givenTopic(givenSyllabus(SyllabusStatus.Drafting));

		service.deleteTopic(SYLLABUS_ID, TOPIC_ID);

		verify(topicRepository).delete(topic);
		verify(auditLogService).record("DELETE_SYLLABUS_TOPIC", "syllabus", SYLLABUS_ID);
	}

	/**
	 * Pins current behaviour: outline edits only write the audit log and never stamp the syllabus's
	 * own updatedAt/updatedBy (the service does not even receive the acting user). Changing that is a
	 * deliberate decision, not a side effect of refactoring.
	 */
	@Test
	void outlineEditLeavesTheSyllabusUpdateStampUnchanged() {
		Syllabus syllabus = givenSyllabus(SyllabusStatus.Drafting);
		LocalDateTime lastUpdatedAt = LocalDateTime.of(2026, 1, 5, 9, 30);
		syllabus.setUpdatedAt(lastUpdatedAt);
		syllabus.setUpdatedBy(3L);

		service.createDay(SYLLABUS_ID, new CreateSyllabusDayRequest(1, 1));

		assertThat(syllabus.getUpdatedAt()).isEqualTo(lastUpdatedAt);
		assertThat(syllabus.getUpdatedBy()).isEqualTo(3L);
		verify(syllabusRepository, never()).save(any());
	}

	private Syllabus givenSyllabus(SyllabusStatus status) {
		Syllabus syllabus = newSyllabus(SYLLABUS_ID, status);
		when(syllabusRepository.findById(SYLLABUS_ID)).thenReturn(Optional.of(syllabus));
		return syllabus;
	}

	private SyllabusDay givenDay(Syllabus syllabus) {
		SyllabusDay day = newDay(DAY_ID, 1, 1);
		day.setSyllabus(syllabus);
		when(dayRepository.findByIdAndSyllabusId(DAY_ID, SYLLABUS_ID)).thenReturn(Optional.of(day));
		return day;
	}

	private SyllabusUnit givenUnit(Syllabus syllabus) {
		SyllabusDay day = newDay(DAY_ID, 1, 1);
		day.setSyllabus(syllabus);
		SyllabusUnit unit = newUnit(UNIT_ID, day, "Basics", 1);
		when(unitRepository.findByIdAndDaySyllabusId(UNIT_ID, SYLLABUS_ID)).thenReturn(Optional.of(unit));
		return unit;
	}

	private SyllabusTopic givenTopic(Syllabus syllabus) {
		SyllabusDay day = newDay(DAY_ID, 1, 1);
		day.setSyllabus(syllabus);
		SyllabusTopic topic = newTopic(TOPIC_ID, newUnit(UNIT_ID, day, "Basics", 1), "Variables", "H1SD");
		when(topicRepository.findByIdAndUnitDaySyllabusId(TOPIC_ID, SYLLABUS_ID)).thenReturn(Optional.of(topic));
		return topic;
	}

	/** The day, unit and topic ids exist, but only when looked up through another syllabus. */
	private void givenOutlineOwnedByAnotherSyllabus() {
		SyllabusDay day = newDay(DAY_ID, 1, 1);
		day.setSyllabus(newSyllabus(OTHER_SYLLABUS_ID, SyllabusStatus.Drafting));
		SyllabusUnit unit = newUnit(UNIT_ID, day, "Basics", 1);
		SyllabusTopic topic = newTopic(TOPIC_ID, unit, "Variables", "H1SD");
		when(dayRepository.findByIdAndSyllabusId(DAY_ID, OTHER_SYLLABUS_ID)).thenReturn(Optional.of(day));
		when(unitRepository.findByIdAndDaySyllabusId(UNIT_ID, OTHER_SYLLABUS_ID)).thenReturn(Optional.of(unit));
		when(topicRepository.findByIdAndUnitDaySyllabusId(TOPIC_ID, OTHER_SYLLABUS_ID)).thenReturn(Optional.of(topic));
	}

	private void verifyNothingWritten() {
		verify(dayRepository, never()).save(any());
		verify(dayRepository, never()).delete(any());
		verify(unitRepository, never()).save(any());
		verify(unitRepository, never()).delete(any());
		verify(topicRepository, never()).save(any());
		verify(topicRepository, never()).delete(any());
		verifyNoInteractions(auditLogService);
	}

	private static Named<OutlineMutation> mutation(String name, OutlineMutation mutation) {
		return Named.of(name, mutation);
	}

	private static CreateSyllabusDayRequest dayRequest() {
		return new CreateSyllabusDayRequest(2, 2);
	}

	private static CreateSyllabusUnitRequest unitRequest() {
		return new CreateSyllabusUnitRequest("OOP", 2);
	}

	private static CreateSyllabusTopicRequest topicRequest() {
		return new CreateSyllabusTopicRequest("Generics", "K6SD", true, 30, SyllabusTopicStatus.Active, 2);
	}

	private static Syllabus newSyllabus(long id, SyllabusStatus status) {
		Syllabus syllabus = new Syllabus();
		syllabus.setId(id);
		syllabus.setCode("JAVA-" + id);
		syllabus.setStatus(status);
		return syllabus;
	}

	private static SyllabusDay newDay(long id, int dayNumber, int sortOrder) {
		SyllabusDay day = new SyllabusDay();
		day.setId(id);
		day.setDayNumber(dayNumber);
		day.setSortOrder(sortOrder);
		return day;
	}

	private static SyllabusUnit newUnit(long id, SyllabusDay day, String name, int sortOrder) {
		SyllabusUnit unit = new SyllabusUnit();
		unit.setId(id);
		unit.setDay(day);
		unit.setName(name);
		unit.setSortOrder(sortOrder);
		return unit;
	}

	private static SyllabusTopic newTopic(long id, SyllabusUnit unit, String name, String outputStandard) {
		SyllabusTopic topic = new SyllabusTopic();
		topic.setId(id);
		topic.setUnit(unit);
		topic.setName(name);
		topic.setOutputStandard(outputStandard);
		topic.setSortOrder(1);
		return topic;
	}
}
