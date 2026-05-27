package com.fap.clazz.repository;

import com.fap.clazz.entity.ClassAdmin;
import com.fap.clazz.entity.ClassAdminId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassAdminRepository extends JpaRepository<ClassAdmin, ClassAdminId> {

	@EntityGraph(attributePaths = "user")
	List<ClassAdmin> findByFapClassIdOrderByUserFullNameAsc(Long classId);

	void deleteByFapClassId(Long classId);

	boolean existsByFapClassId(Long classId);
}
