package com.fap.training.repository;

import com.fap.training.enums.TrainingSessionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingSessionRepositoryQueryTest {

	@Test
	void scopedSearchAvoidsDistinctAcrossOracleClobColumns() throws NoSuchMethodException {
		Query query = TrainingSessionRepository.class
				.getMethod(
						"searchScoped",
						Long.class,
						TrainingSessionStatus.class,
						Long.class,
						Long.class,
						LocalDate.class,
						LocalDate.class,
						String.class,
						Pageable.class)
				.getAnnotation(Query.class);

		assertThat(query.value())
				.contains("exists (select ca.id", "exists (select ct.id")
				.doesNotContain("select distinct s");
	}
}
