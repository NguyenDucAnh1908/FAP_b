package com.fap.result.mapper;

import com.fap.clazz.entity.FapClass;
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
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pure mapping: callers load the quiz snapshots and adjustments, so listing a class stays a
 * fixed number of queries however many results it has.
 */
@Component
public class CourseResultMapper {

	public CompletionPolicyResponse toPolicyResponse(FapClass fapClass, List<ClassCompletionQuiz> quizzes) {
		return new CompletionPolicyResponse(
				fapClass.getId(),
				fapClass.getMinimumAttendanceRate(),
				quizzes.stream().map(item -> new CompletionPolicyQuizResponse(
						item.getQuiz().getId(),
						item.getQuiz().getTitle(),
						item.getPassingScore(),
						item.getQuiz().getStatus())).toList());
	}

	public ClassCourseResultsResponse toClassResultsResponse(
			Long classId,
			FapClass fapClass,
			List<CourseResultResponse> results) {
		return new ClassCourseResultsResponse(
				classId,
				fapClass.getName(),
				fapClass.getClassCode(),
				toSummary(results),
				results);
	}

	public CourseResultResponse toResponse(
			CourseResult result,
			List<CourseResultQuiz> resultQuizzes,
			List<CourseResultAdjustment> resultAdjustments) {
		List<CourseResultQuizResponse> quizzes = resultQuizzes.stream()
				.map(item -> new CourseResultQuizResponse(
						item.getQuiz().getId(),
						item.getQuiz().getTitle(),
						item.getRequiredScore(),
						item.getBestAttemptId(),
						item.getBestScore(),
						item.isPassed()))
				.toList();
		List<CourseResultAdjustmentResponse> adjustments = resultAdjustments.stream()
				.map(item -> new CourseResultAdjustmentResponse(
						item.getId(), item.getPreviousStatus(), item.getNewStatus(), item.getReason(),
						item.getAdjustedBy(), item.getAdjustedAt()))
				.toList();
		return new CourseResultResponse(
				result.getId(),
				result.getFapClass().getId(),
				result.getClassEnrollment().getId(),
				result.getClassEnrollment().getUser().getId(),
				result.getClassEnrollment().getUser().getFullName(),
				result.getClassEnrollment().getUser().getEmail(),
				result.effectiveStatus(),
				result.getCalculatedStatus(),
				result.getOverrideStatus(),
				result.getAttendanceRate(),
				result.getAttendedSessions(),
				result.getTotalSessions(),
				result.getRequiredQuizCount(),
				result.getPassedQuizCount(),
				result.getOverrideReason(),
				result.getOverriddenBy(),
				result.getOverriddenAt(),
				result.getPublishedAt() != null,
				result.getPublishedAt(),
				result.getCalculatedAt(),
				result.getVersionNo(),
				quizzes,
				adjustments);
	}

	private CourseResultSummaryResponse toSummary(List<CourseResultResponse> results) {
		return new CourseResultSummaryResponse(
				results.size(),
				results.stream().filter(item -> item.status() == CourseResultStatus.InProgress).count(),
				results.stream().filter(item -> item.status() == CourseResultStatus.Passed).count(),
				results.stream().filter(item -> item.status() == CourseResultStatus.Failed).count(),
				results.stream().filter(item -> item.status() == CourseResultStatus.Withdrawn).count(),
				results.stream().filter(CourseResultResponse::published).count());
	}
}
