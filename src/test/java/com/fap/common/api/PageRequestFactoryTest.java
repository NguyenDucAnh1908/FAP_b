package com.fap.common.api;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PageRequestFactoryTest {

	@Test
	void mapsEntitySortFieldsToNativeQueryColumns() {
		PageRequest pageRequest = PageRequest.of(
				1,
				20,
				Sort.by(
						Sort.Order.desc("createdAt"),
						Sort.Order.asc("status")));

		PageRequest result = PageRequestFactory.mapSortFields(pageRequest, Map.of(
				"createdAt", "created_at"));

		assertThat(result.getPageNumber()).isEqualTo(1);
		assertThat(result.getPageSize()).isEqualTo(20);
		assertThat(result.getSort().getOrderFor("created_at").getDirection()).isEqualTo(Sort.Direction.DESC);
		assertThat(result.getSort().getOrderFor("status").getDirection()).isEqualTo(Sort.Direction.ASC);
		assertThat(result.getSort().getOrderFor("createdAt")).isNull();
	}
}
