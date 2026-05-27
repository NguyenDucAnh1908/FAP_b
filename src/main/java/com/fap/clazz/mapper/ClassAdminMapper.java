package com.fap.clazz.mapper;

import com.fap.clazz.dto.ClassAdminResponse;
import com.fap.clazz.entity.ClassAdmin;
import com.fap.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ClassAdminMapper {

	public ClassAdminResponse toResponse(ClassAdmin classAdmin) {
		User user = classAdmin.getUser();
		return new ClassAdminResponse(
				user.getId(),
				user.getFullName(),
				user.getEmail());
	}
}
