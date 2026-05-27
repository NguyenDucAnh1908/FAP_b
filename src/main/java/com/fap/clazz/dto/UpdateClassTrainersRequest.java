package com.fap.clazz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateClassTrainersRequest(
		@NotEmpty List<@Valid ClassTrainerItemRequest> trainers
) {
}
