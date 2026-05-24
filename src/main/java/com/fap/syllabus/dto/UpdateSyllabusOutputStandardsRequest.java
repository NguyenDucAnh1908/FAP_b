package com.fap.syllabus.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.Set;

public record UpdateSyllabusOutputStandardsRequest(
		@NotEmpty Set<@Pattern(regexp = "H4SD|K6SD|H1SD|C3SD|H2SD") String> standards
) {
}
