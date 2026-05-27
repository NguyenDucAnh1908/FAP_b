package com.fap.clazz.mapper;

import com.fap.clazz.dto.ClassTrainerResponse;
import com.fap.clazz.entity.ClassTrainer;
import com.fap.syllabus.entity.Syllabus;
import com.fap.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ClassTrainerMapper {

	public ClassTrainerResponse toResponse(ClassTrainer classTrainer) {
		User user = classTrainer.getUser();
		Syllabus syllabus = classTrainer.getSyllabus();
		return new ClassTrainerResponse(
				classTrainer.getId(),
				user.getId(),
				user.getFullName(),
				user.getEmail(),
				syllabus == null ? null : syllabus.getId(),
				syllabus == null ? null : syllabus.getName(),
				syllabus == null ? null : syllabus.getCode());
	}
}
