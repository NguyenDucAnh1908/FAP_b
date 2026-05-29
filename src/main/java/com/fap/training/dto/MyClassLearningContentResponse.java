package com.fap.training.dto;

import com.fap.clazz.dto.ClassResponse;
import com.fap.quiz.dto.AssignedQuizResponse;
import com.fap.syllabus.dto.AssignedMaterialFileResponse;

import java.util.List;

public record MyClassLearningContentResponse(
		ClassResponse classInfo,
		List<MyClassSyllabusResponse> syllabuses,
		List<MyTrainingSessionResponse> sessions,
		List<AssignedMaterialFileResponse> materials,
		List<AssignedQuizResponse> quizzes
) {
}
