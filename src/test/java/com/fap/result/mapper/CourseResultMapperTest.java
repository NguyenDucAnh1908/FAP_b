package com.fap.result.mapper;

import com.fap.clazz.entity.ClassEnrollment;
import com.fap.clazz.entity.FapClass;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.enums.QuizStatus;
import com.fap.result.dto.ClassCourseResultsResponse;
import com.fap.result.dto.CompletionPolicyQuizResponse;
import com.fap.result.dto.CompletionPolicyResponse;
import com.fap.result.dto.CourseResultAdjustmentResponse;
import com.fap.result.dto.CourseResultQuizResponse;
import com.fap.result.dto.CourseResultResponse;
import com.fap.result.dto.CourseResultSummaryResponse;
import com.fap.result.entity.ClassCompletionQuiz;
import com.fap.result.entity.CourseResult;
import com.fap.result.entity.CourseResultAdjustment;
import com.fap.result.entity.CourseResultQuiz;
import com.fap.result.enums.CourseResultStatus;
import com.fap.user.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourseResultMapperTest {
	private static final Long CLASS_ID = 10L;

	private final CourseResultMapper mapper = new CourseResultMapper();

	@Test
	void resultShowsTheOverrideAsItsStatusAndCarriesSnapshotsAndHistory() {
		FapClass fapClass = givenClass();
		CourseResult result = givenResult(fapClass, 500L, 20L, CourseResultStatus.Failed);
		result.setOverrideStatus(CourseResultStatus.Passed);
		result.setOverrideReason("Approved after review");
		result.setOverriddenBy(30L);
		result.setAttendanceRate(new BigDecimal("75.00"));
		result.setAttendedSessions(3);
		result.setTotalSessions(4);
		result.setRequiredQuizCount(1);
		result.setPassedQuizCount(0);
		result.setVersionNo(2L);
		CourseResultQuiz snapshot = new CourseResultQuiz();
		snapshot.setQuiz(givenQuiz());
		snapshot.setRequiredScore(70);
		snapshot.setBestAttemptId(400L);
		snapshot.setBestScore(65);
		snapshot.setPassed(false);
		LocalDateTime adjustedAt = LocalDateTime.of(2026, 1, 5, 9, 30);
		CourseResultAdjustment adjustment = new CourseResultAdjustment();
		adjustment.setId(600L);
		adjustment.setPreviousStatus(CourseResultStatus.Failed);
		adjustment.setNewStatus(CourseResultStatus.Passed);
		adjustment.setReason("Approved after review");
		adjustment.setAdjustedBy(30L);
		adjustment.setAdjustedAt(adjustedAt);

		CourseResultResponse response = mapper.toResponse(result, List.of(snapshot), List.of(adjustment));

		assertThat(response.id()).isEqualTo(500L);
		assertThat(response.classId()).isEqualTo(CLASS_ID);
		assertThat(response.enrollmentId()).isEqualTo(result.getClassEnrollment().getId());
		assertThat(response.userId()).isEqualTo(20L);
		assertThat(response.traineeName()).isEqualTo("Trainee 20");
		assertThat(response.traineeEmail()).isEqualTo("trainee20@example.com");
		assertThat(response.status()).isEqualTo(CourseResultStatus.Passed);
		assertThat(response.calculatedStatus()).isEqualTo(CourseResultStatus.Failed);
		assertThat(response.overrideStatus()).isEqualTo(CourseResultStatus.Passed);
		assertThat(response.attendanceRate()).isEqualTo(new BigDecimal("75.00"));
		assertThat(response.attendedSessions()).isEqualTo(3);
		assertThat(response.totalSessions()).isEqualTo(4);
		assertThat(response.overrideReason()).isEqualTo("Approved after review");
		assertThat(response.published()).isFalse();
		assertThat(response.version()).isEqualTo(2L);
		assertThat(response.quizzes()).containsExactly(
				new CourseResultQuizResponse(300L, "Final quiz", 70, 400L, 65, false));
		assertThat(response.adjustments()).containsExactly(new CourseResultAdjustmentResponse(
				600L, CourseResultStatus.Failed, CourseResultStatus.Passed, "Approved after review", 30L, adjustedAt));
	}

	@Test
	void resultIsPublishedOnlyOnceItHasAPublicationTime() {
		CourseResult result = givenResult(givenClass(), 500L, 20L, CourseResultStatus.Passed);
		LocalDateTime publishedAt = LocalDateTime.of(2026, 2, 1, 8, 0);
		result.setPublishedAt(publishedAt);

		CourseResultResponse response = mapper.toResponse(result, List.of(), List.of());

		assertThat(response.published()).isTrue();
		assertThat(response.publishedAt()).isEqualTo(publishedAt);
		assertThat(response.quizzes()).isEmpty();
		assertThat(response.adjustments()).isEmpty();
	}

	@Test
	void classResultsSummaryCountsEffectiveStatusesAndPublications() {
		FapClass fapClass = givenClass();
		CourseResult inProgress = givenResult(fapClass, 501L, 21L, CourseResultStatus.InProgress);
		CourseResult passed = givenResult(fapClass, 502L, 22L, CourseResultStatus.Passed);
		passed.setPublishedAt(LocalDateTime.of(2026, 2, 1, 8, 0));
		CourseResult overriddenToPassed = givenResult(fapClass, 503L, 23L, CourseResultStatus.Failed);
		overriddenToPassed.setOverrideStatus(CourseResultStatus.Passed);
		CourseResult failed = givenResult(fapClass, 504L, 24L, CourseResultStatus.Failed);
		CourseResult withdrawn = givenResult(fapClass, 505L, 25L, CourseResultStatus.Withdrawn);
		List<CourseResultResponse> results = List.of(inProgress, passed, overriddenToPassed, failed, withdrawn).stream()
				.map(result -> mapper.toResponse(result, List.of(), List.of()))
				.toList();

		ClassCourseResultsResponse response = mapper.toClassResultsResponse(CLASS_ID, fapClass, results);

		assertThat(response.classId()).isEqualTo(CLASS_ID);
		assertThat(response.className()).isEqualTo("Java Fundamentals");
		assertThat(response.classCode()).isEqualTo("JAVA-01");
		assertThat(response.summary()).isEqualTo(new CourseResultSummaryResponse(5, 1, 2, 1, 1, 1));
		assertThat(response.results()).isSameAs(results);
	}

	@Test
	void policyListsRequiredQuizzesWithTheirCurrentStatus() {
		FapClass fapClass = givenClass();
		fapClass.setMinimumAttendanceRate(new BigDecimal("85.00"));
		ClassCompletionQuiz requiredQuiz = new ClassCompletionQuiz();
		requiredQuiz.setQuiz(givenQuiz());
		requiredQuiz.setPassingScore(70);

		CompletionPolicyResponse response = mapper.toPolicyResponse(fapClass, List.of(requiredQuiz));

		assertThat(response).isEqualTo(new CompletionPolicyResponse(
				CLASS_ID,
				new BigDecimal("85.00"),
				List.of(new CompletionPolicyQuizResponse(300L, "Final quiz", 70, QuizStatus.Closed))));
	}

	private static FapClass givenClass() {
		FapClass fapClass = new FapClass();
		fapClass.setId(CLASS_ID);
		fapClass.setName("Java Fundamentals");
		fapClass.setClassCode("JAVA-01");
		return fapClass;
	}

	private static Quiz givenQuiz() {
		Quiz quiz = new Quiz();
		quiz.setId(300L);
		quiz.setTitle("Final quiz");
		quiz.setStatus(QuizStatus.Closed);
		return quiz;
	}

	private static CourseResult givenResult(FapClass fapClass, Long id, Long userId, CourseResultStatus status) {
		User user = new User();
		user.setId(userId);
		user.setFullName("Trainee " + userId);
		user.setEmail("trainee" + userId + "@example.com");
		ClassEnrollment enrollment = new ClassEnrollment();
		enrollment.setId(id + 1000);
		enrollment.setFapClass(fapClass);
		enrollment.setUser(user);
		CourseResult result = new CourseResult();
		result.setId(id);
		result.setFapClass(fapClass);
		result.setClassEnrollment(enrollment);
		result.setCalculatedStatus(status);
		return result;
	}
}
