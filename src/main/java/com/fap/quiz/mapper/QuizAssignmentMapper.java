package com.fap.quiz.mapper;

import com.fap.clazz.entity.FapClass;
import com.fap.quiz.dto.QuizAssignmentResponse;
import com.fap.quiz.entity.QuizAssignment;
import com.fap.training.entity.TrainingSession;
import com.fap.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class QuizAssignmentMapper {

	public QuizAssignmentResponse toResponse(QuizAssignment assignment) {
		FapClass fapClass = assignment.getFapClass();
		TrainingSession trainingSession = assignment.getTrainingSession();
		User assignedBy = assignment.getAssignedBy();
		return new QuizAssignmentResponse(
				assignment.getId(),
				assignment.getQuiz().getId(),
				assignment.getQuiz().getTitle(),
				fapClass == null ? null : fapClass.getId(),
				fapClass == null ? null : fapClass.getName(),
				fapClass == null ? null : fapClass.getClassCode(),
				trainingSession == null ? null : trainingSession.getId(),
				trainingSession == null ? null : trainingSession.getTitle(),
				assignedBy.getId(),
				assignedBy.getFullName(),
				assignedBy.getEmail(),
				assignment.getAssignedAt());
	}
}
