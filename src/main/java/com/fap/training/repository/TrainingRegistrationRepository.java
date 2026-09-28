package com.fap.training.repository;

import com.fap.common.exception.NotFoundException;
import com.fap.training.entity.TrainingRegistration;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrainingRegistrationRepository extends JpaRepository<TrainingRegistration, Long> {

	@Query("""
			select r.status as status, count(r) as total
			from TrainingRegistration r
			where (:classAdminId is null
			       or exists (select ca.id from ClassAdmin ca
			                  where ca.fapClass = r.trainingSession.fapClass
			                    and ca.user.id = :classAdminId))
			  and (:fromDate is null or r.trainingSession.sessionDate >= :fromDate)
			  and (:toDate is null or r.trainingSession.sessionDate <= :toDate)
			group by r.status
			""")
	List<AnalyticsRegistrationStatusCount> countStatusesForAnalytics(
			@Param("classAdminId") Long classAdminId,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate);

	@Query("""
			select count(distinct r.user.id)
			from TrainingRegistration r
			where r.status in :statuses
			  and (:classAdminId is null
			       or exists (select ca.id from ClassAdmin ca
			                  where ca.fapClass = r.trainingSession.fapClass
			                    and ca.user.id = :classAdminId))
			  and (:fromDate is null or r.trainingSession.sessionDate >= :fromDate)
			  and (:toDate is null or r.trainingSession.sessionDate <= :toDate)
			""")
	long countDistinctParticipantsForAnalytics(
			@Param("classAdminId") Long classAdminId,
			@Param("statuses") Collection<TrainingRegistrationStatus> statuses,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate);

	interface AnalyticsRegistrationStatusCount {
		TrainingRegistrationStatus getStatus();

		Long getTotal();
	}

	@EntityGraph(attributePaths = {"trainingSession", "user"})
	Optional<TrainingRegistration> findByTrainingSessionIdAndUserId(Long trainingSessionId, Long userId);

	/**
	 * Lookup for callers that treat a missing registration as not found. Feedback submission uses
	 * the same finder but answers with a conflict instead, so it stays on the finder.
	 */
	default TrainingRegistration getByTrainingSessionIdAndUserIdOrThrow(Long trainingSessionId, Long userId) {
		return findByTrainingSessionIdAndUserId(trainingSessionId, userId)
				.orElseThrow(() -> new NotFoundException("Training registration not found"));
	}

	/** All registrations of a session, to sync many users against it without a lookup per user. */
	List<TrainingRegistration> findByTrainingSessionId(Long trainingSessionId);

	/** One user's registrations across sessions, to sync that user without a lookup per session. */
	List<TrainingRegistration> findByUserIdAndTrainingSessionIdIn(Long userId, Collection<Long> trainingSessionIds);

	@EntityGraph(attributePaths = {"trainingSession", "user"})
	Optional<TrainingRegistration> findByTrainingSessionIdAndUserIdAndStatus(
			Long trainingSessionId, Long userId, TrainingRegistrationStatus status);

	@EntityGraph(attributePaths = {"trainingSession", "user"})
	List<TrainingRegistration> findByTrainingSessionIdAndStatusInOrderByRegisteredAtAscIdAsc(
			Long trainingSessionId,
			Collection<TrainingRegistrationStatus> statuses);

	@EntityGraph(attributePaths = {"trainingSession", "user"})
	Optional<TrainingRegistration> findFirstByTrainingSessionIdAndStatusOrderByRegisteredAtAscIdAsc(
			Long trainingSessionId,
			TrainingRegistrationStatus status);

	@EntityGraph(attributePaths = {"trainingSession", "trainingSession.fapClass", "trainingSession.trainer", "user"})
	@Query("""
			select r
			from TrainingRegistration r
			where r.user.id = :userId
			  and (:registrationStatus is null or r.status = :registrationStatus)
			  and (:sessionStatus is null or r.trainingSession.status = :sessionStatus)
			  and (:fromDate is null or r.trainingSession.sessionDate >= :fromDate)
			  and (:toDate is null or r.trainingSession.sessionDate <= :toDate)
			  and (:keyword is null
			       or lower(r.trainingSession.title) like concat(concat('%', lower(:keyword)), '%')
			       or lower(coalesce(r.trainingSession.room, '')) like concat(concat('%', lower(:keyword)), '%')
			       or lower(r.trainingSession.fapClass.name) like concat(concat('%', lower(:keyword)), '%')
			       or lower(r.trainingSession.fapClass.classCode) like concat(concat('%', lower(:keyword)), '%')
			       or lower(r.trainingSession.trainer.fullName) like concat(concat('%', lower(:keyword)), '%')
			       or lower(r.trainingSession.trainer.email) like concat(concat('%', lower(:keyword)), '%'))
			order by r.trainingSession.sessionDate asc, r.trainingSession.startTime asc, r.id asc
			""")
	Page<TrainingRegistration> searchMine(
			@Param("userId") Long userId,
			@Param("registrationStatus") TrainingRegistrationStatus registrationStatus,
			@Param("sessionStatus") TrainingSessionStatus sessionStatus,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate,
			@Param("keyword") String keyword,
			Pageable pageable);

	@Query("""
			select count(r)
			from TrainingRegistration r
			where r.user.id = :userId
			  and (:registrationStatus is null or r.status = :registrationStatus)
			  and (:sessionStatus is null or r.trainingSession.status = :sessionStatus)
			  and (:fromDate is null or r.trainingSession.sessionDate >= :fromDate)
			  and (:toDate is null or r.trainingSession.sessionDate <= :toDate)
			""")
	long countMine(
			@Param("userId") Long userId,
			@Param("registrationStatus") TrainingRegistrationStatus registrationStatus,
			@Param("sessionStatus") TrainingSessionStatus sessionStatus,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate);

	@Query("""
			select count(r)
			from TrainingRegistration r
			join ClassAdmin ca on ca.fapClass = r.trainingSession.fapClass
			where ca.user.id = :adminId
			  and (:registrationStatus is null or r.status = :registrationStatus)
			""")
	long countByClassAdminIdAndStatus(
			@Param("adminId") Long adminId,
			@Param("registrationStatus") TrainingRegistrationStatus registrationStatus);

	long countByTrainingSessionIdAndStatus(Long trainingSessionId, TrainingRegistrationStatus status);

	@Query("""
			select r.user.id
			from TrainingRegistration r
			where r.trainingSession.id = :trainingSessionId
			  and r.status = :status
			""")
	List<Long> findUserIdsByTrainingSessionIdAndStatus(
			@Param("trainingSessionId") Long trainingSessionId,
			@Param("status") TrainingRegistrationStatus status);

	/** Registrations of every user in a class, for per-class calculations that would otherwise query per user. */
	@EntityGraph(attributePaths = "trainingSession")
	@Query("""
			select r
			from TrainingRegistration r
			where r.trainingSession.fapClass.id = :classId
			  and r.status in :statuses
			""")
	List<TrainingRegistration> findByClassIdAndStatusIn(
			@Param("classId") Long classId,
			@Param("statuses") Collection<TrainingRegistrationStatus> statuses);

	@EntityGraph(attributePaths = {"trainingSession", "trainingSession.fapClass", "trainingSession.trainer", "user"})
	@Query("""
			select r
			from TrainingRegistration r
			where r.user.id = :userId
			  and r.trainingSession.fapClass.id = :classId
			  and r.status in :eligibleStatuses
			order by r.trainingSession.sessionDate asc, r.trainingSession.startTime asc, r.id asc
			""")
	List<TrainingRegistration> findMineByClassId(
			@Param("userId") Long userId,
			@Param("classId") Long classId,
			@Param("eligibleStatuses") Collection<TrainingRegistrationStatus> eligibleStatuses);

	@EntityGraph(attributePaths = {"trainingSession", "trainingSession.fapClass", "trainingSession.trainer", "user"})
	@Query("""
			select r
			from TrainingRegistration r
			where r.user.id = :userId
			  and r.trainingSession.fapClass.id = :classId
			  and r.trainingSession.status = :sessionStatus
			  and r.status in :registrationStatuses
			order by r.trainingSession.sessionDate asc, r.trainingSession.startTime asc, r.id asc
			""")
	List<TrainingRegistration> findFutureByClassAndUser(
			@Param("classId") Long classId,
			@Param("userId") Long userId,
			@Param("sessionStatus") TrainingSessionStatus sessionStatus,
			@Param("registrationStatuses") Collection<TrainingRegistrationStatus> registrationStatuses);
}
