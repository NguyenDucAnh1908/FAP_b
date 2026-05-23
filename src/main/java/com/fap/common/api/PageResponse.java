package com.fap.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageResponse<T>(
		boolean success,
		List<T> data,
		Pagination pagination
) {

	public static <T> PageResponse<T> of(List<T> data, int page, int limit, long total) {
		int totalPages = limit <= 0 ? 0 : (int) Math.ceil((double) total / limit);
		return new PageResponse<>(true, data, new Pagination(page, limit, total, totalPages));
	}

	public record Pagination(
			int page,
			int limit,
			long total,
			int totalPages
	) {
	}
}
