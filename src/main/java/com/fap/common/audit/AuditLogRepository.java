package com.fap.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	@Query("""
			select a
			from AuditLog a
			where (:userId is null or a.userId = :userId)
			  and (:entityType is null or lower(a.entityType) = lower(:entityType))
			  and (:entityId is null or a.entityId = :entityId)
			""")
	Page<AuditLog> search(
			@Param("userId") Long userId,
			@Param("entityType") String entityType,
			@Param("entityId") Long entityId,
			Pageable pageable);
}
