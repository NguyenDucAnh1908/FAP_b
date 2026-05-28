package com.fap.clazz.repository;

import com.fap.clazz.entity.ClassTrainer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassTrainerRepository extends JpaRepository<ClassTrainer, Long> {

	@EntityGraph(attributePaths = {"user", "syllabus"})
	List<ClassTrainer> findByFapClassIdOrderByIdAsc(Long classId);

	void deleteByFapClassId(Long classId);

	boolean existsByFapClassId(Long classId);

	boolean existsByFapClassIdAndUserId(Long classId, Long userId);

	@Query("""
			select count(distinct ct.fapClass.id)
			from ClassTrainer ct
			where ct.user.id = :trainerId
			""")
	long countDistinctClassesByTrainerId(@Param("trainerId") Long trainerId);

	@Query("""
			select count(distinct ct.user.id)
			from ClassTrainer ct
			join ClassAdmin ca on ca.fapClass = ct.fapClass
			where ca.user.id = :adminId
			""")
	long countDistinctTrainersByClassAdminId(@Param("adminId") Long adminId);
}
