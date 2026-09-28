package com.fap.clazz.service;

import com.fap.clazz.entity.FapClass;
import com.fap.clazz.mapper.ClassMapper;
import com.fap.clazz.repository.ClassAdminRepository;
import com.fap.clazz.repository.ClassRepository;
import com.fap.clazz.repository.ClassTrainerRepository;
import com.fap.common.audit.AuditLogService;
import com.fap.program.repository.TrainingProgramRepository;
import com.fap.result.service.CourseResultService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ClassMinimumAttendanceRateTest {

	private static final Long ACTOR_ID = 7L;
	private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2026, 3, 15, 9, 0);

	private final ClassRepository classRepository = mock(ClassRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final ClassService service = new ClassService(
			classRepository,
			mock(ClassAdminRepository.class),
			mock(ClassTrainerRepository.class),
			mock(TrainingProgramRepository.class),
			mock(ClassMapper.class),
			auditLogService,
			mock(ClassEnrollmentService.class),
			mock(CourseResultService.class));

	@Test
	void minimumAttendanceRateIsRoundedToTwoDecimalsAndStampedOnTheLockedClass() {
		FapClass lockedClass = new FapClass();
		lockedClass.setId(21L);

		service.updateMinimumAttendanceRate(lockedClass, new BigDecimal("75.555"), ACTOR_ID, UPDATED_AT);

		assertThat(lockedClass.getMinimumAttendanceRate()).isEqualTo(new BigDecimal("75.56"));
		assertThat(lockedClass.getUpdatedAt()).isEqualTo(UPDATED_AT);
		assertThat(lockedClass.getUpdatedBy()).isEqualTo(ACTOR_ID);
		// Writes the instance the caller locked, without reloading it; the caller audits the policy change.
		verifyNoInteractions(classRepository, auditLogService);
	}

	@Test
	void minimumAttendanceRateRoundsHalfUpRatherThanToEven() {
		FapClass lockedClass = new FapClass();

		service.updateMinimumAttendanceRate(lockedClass, new BigDecimal("75.565"), ACTOR_ID, UPDATED_AT);

		assertThat(lockedClass.getMinimumAttendanceRate()).isEqualTo(new BigDecimal("75.57"));
	}
}
