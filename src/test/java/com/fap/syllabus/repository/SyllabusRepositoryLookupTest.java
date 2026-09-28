package com.fap.syllabus.repository;

import com.fap.common.exception.NotFoundException;
import com.fap.syllabus.entity.MaterialFile;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.entity.SyllabusDay;
import com.fap.syllabus.entity.SyllabusTopic;
import com.fap.syllabus.entity.SyllabusUnit;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The {@code get...OrThrow} defaults replace lookups that were copied into several services, so they
 * must keep each call site's exact exception and message, and delegate to the same finder (same SQL).
 */
class SyllabusRepositoryLookupTest {

	private static final long ID = 5L;
	private static final long PARENT_ID = 9L;

	@Test
	void syllabusLookupReturnsTheFoundSyllabus() {
		SyllabusRepository repository = mock(SyllabusRepository.class);
		doCallRealMethod().when(repository).getOrThrow(any());
		Syllabus syllabus = new Syllabus();
		when(repository.findById(ID)).thenReturn(Optional.of(syllabus));

		assertThat(repository.getOrThrow(ID)).isSameAs(syllabus);
	}

	@Test
	void missingSyllabusIsNotFound() {
		SyllabusRepository repository = mock(SyllabusRepository.class);
		doCallRealMethod().when(repository).getOrThrow(any());
		when(repository.findById(ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> repository.getOrThrow(ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Syllabus not found");
	}

	@Test
	void dayLookupIsScopedToTheSyllabus() {
		SyllabusDayRepository repository = mock(SyllabusDayRepository.class);
		doCallRealMethod().when(repository).getByIdAndSyllabusIdOrThrow(any(), any());
		SyllabusDay day = new SyllabusDay();
		when(repository.findByIdAndSyllabusId(ID, PARENT_ID)).thenReturn(Optional.of(day));

		assertThat(repository.getByIdAndSyllabusIdOrThrow(ID, PARENT_ID)).isSameAs(day);
		assertThatThrownBy(() -> repository.getByIdAndSyllabusIdOrThrow(PARENT_ID, ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Syllabus day not found");
	}

	@Test
	void unitLookupIsScopedToTheSyllabus() {
		SyllabusUnitRepository repository = mock(SyllabusUnitRepository.class);
		doCallRealMethod().when(repository).getByIdAndDaySyllabusIdOrThrow(any(), any());
		SyllabusUnit unit = new SyllabusUnit();
		when(repository.findByIdAndDaySyllabusId(ID, PARENT_ID)).thenReturn(Optional.of(unit));

		assertThat(repository.getByIdAndDaySyllabusIdOrThrow(ID, PARENT_ID)).isSameAs(unit);
		assertThatThrownBy(() -> repository.getByIdAndDaySyllabusIdOrThrow(PARENT_ID, ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Syllabus unit not found");
	}

	@Test
	void topicLookupIsScopedToTheSyllabus() {
		SyllabusTopicRepository repository = mock(SyllabusTopicRepository.class);
		doCallRealMethod().when(repository).getByIdAndUnitDaySyllabusIdOrThrow(any(), any());
		SyllabusTopic topic = new SyllabusTopic();
		when(repository.findByIdAndUnitDaySyllabusId(ID, PARENT_ID)).thenReturn(Optional.of(topic));

		assertThat(repository.getByIdAndUnitDaySyllabusIdOrThrow(ID, PARENT_ID)).isSameAs(topic);
		assertThatThrownBy(() -> repository.getByIdAndUnitDaySyllabusIdOrThrow(PARENT_ID, ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Syllabus topic not found");
	}

	@Test
	void materialLookupsShareTheNotFoundMessage() {
		MaterialFileRepository repository = mock(MaterialFileRepository.class);
		doCallRealMethod().when(repository).getWithTopicOrThrow(any());
		doCallRealMethod().when(repository).getByIdAndTopicIdOrThrow(any(), any());
		MaterialFile material = new MaterialFile();
		when(repository.findWithTopicById(ID)).thenReturn(Optional.of(material));
		when(repository.findByIdAndTopicId(ID, PARENT_ID)).thenReturn(Optional.of(material));

		assertThat(repository.getWithTopicOrThrow(ID)).isSameAs(material);
		assertThat(repository.getByIdAndTopicIdOrThrow(ID, PARENT_ID)).isSameAs(material);
		assertThatThrownBy(() -> repository.getWithTopicOrThrow(PARENT_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Material file not found");
		assertThatThrownBy(() -> repository.getByIdAndTopicIdOrThrow(PARENT_ID, ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Material file not found");
	}
}
