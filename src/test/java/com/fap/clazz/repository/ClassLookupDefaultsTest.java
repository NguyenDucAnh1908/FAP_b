package com.fap.clazz.repository;

import com.fap.clazz.entity.ClassEnrollment;
import com.fap.clazz.entity.FapClass;
import com.fap.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The lookup defaults replaced inline {@code orElseThrow} calls in the services, so they must keep
 * the exact exception type and message those call sites threw.
 */
class ClassLookupDefaultsTest {

	private final ClassRepository classRepository = mock(ClassRepository.class);
	private final ClassEnrollmentRepository classEnrollmentRepository = mock(ClassEnrollmentRepository.class);

	@BeforeEach
	void callRealDefaults() {
		doCallRealMethod().when(classRepository).getWithTrainingProgramOrThrow(any());
		doCallRealMethod().when(classRepository).getWithTrainingProgramForUpdateOrThrow(any());
		doCallRealMethod().when(classEnrollmentRepository).getByFapClassIdAndUserIdOrThrow(any(), any());
	}

	@Test
	void classLookupReturnsTheFoundClass() {
		FapClass fapClass = new FapClass();
		when(classRepository.findWithTrainingProgramById(1L)).thenReturn(Optional.of(fapClass));

		assertThat(classRepository.getWithTrainingProgramOrThrow(1L)).isSameAs(fapClass);
	}

	@Test
	void missingClassIsNotFound() {
		when(classRepository.findWithTrainingProgramById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> classRepository.getWithTrainingProgramOrThrow(1L))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Class not found");
	}

	@Test
	void lockingClassLookupReturnsTheFoundClass() {
		FapClass fapClass = new FapClass();
		when(classRepository.findWithTrainingProgramByIdForUpdate(1L)).thenReturn(Optional.of(fapClass));

		assertThat(classRepository.getWithTrainingProgramForUpdateOrThrow(1L)).isSameAs(fapClass);
	}

	@Test
	void missingClassToLockIsNotFound() {
		when(classRepository.findWithTrainingProgramByIdForUpdate(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> classRepository.getWithTrainingProgramForUpdateOrThrow(1L))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Class not found");
	}

	@Test
	void enrollmentLookupReturnsTheFoundEnrollment() {
		ClassEnrollment enrollment = new ClassEnrollment();
		when(classEnrollmentRepository.findByFapClassIdAndUserId(1L, 2L)).thenReturn(Optional.of(enrollment));

		assertThat(classEnrollmentRepository.getByFapClassIdAndUserIdOrThrow(1L, 2L)).isSameAs(enrollment);
	}

	@Test
	void missingEnrollmentIsNotFound() {
		when(classEnrollmentRepository.findByFapClassIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> classEnrollmentRepository.getByFapClassIdAndUserIdOrThrow(1L, 2L))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Class enrollment not found");
	}
}
