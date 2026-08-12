package com.fap.common.api;

import com.fap.common.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public final class PageRequestFactory {

	private PageRequestFactory() {
	}

	public static PageRequest create(
			int page,
			int limit,
			String sortBy,
			String order,
			Sort defaultSort,
			String... allowedSortFields) {
		if (sortBy == null || sortBy.isBlank()) {
			return PageRequest.of(page, limit, defaultSort);
		}

		Set<String> allowedFields = Set.copyOf(Arrays.asList(allowedSortFields));
		String normalizedSortBy = sortBy.trim();
		if (!allowedFields.contains(normalizedSortBy)) {
			throw new BadRequestException(
					"INVALID_SORT_FIELD",
					"Unsupported sort field: " + normalizedSortBy);
		}

		Sort.Direction direction = parseDirection(order);
		return PageRequest.of(page, limit, Sort.by(direction, normalizedSortBy));
	}

	public static PageRequest mapSortFields(PageRequest pageRequest, Map<String, String> fieldMappings) {
		Sort mappedSort = Sort.by(pageRequest.getSort().stream()
				.map(order -> new Sort.Order(
						order.getDirection(),
						fieldMappings.getOrDefault(order.getProperty(), order.getProperty())))
				.toList());
		return PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize(), mappedSort);
	}

	private static Sort.Direction parseDirection(String order) {
		if (order == null || order.isBlank()) {
			return Sort.Direction.ASC;
		}
		try {
			return Sort.Direction.fromString(order.trim());
		} catch (IllegalArgumentException exception) {
			throw new BadRequestException(
					"INVALID_SORT_ORDER",
					"Sort order must be asc or desc");
		}
	}
}
