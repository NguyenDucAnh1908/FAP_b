package com.fap.result.service;

import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.repository.ClassRepository;
import com.fap.clazz.service.ClassService;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.repository.QuizAssignmentRepository;
import com.fap.quiz.repository.QuizRepository;
import com.fap.result.dto.CompletionPolicyQuizRequest;
import com.fap.result.dto.CompletionPolicyResponse;
import com.fap.result.dto.UpdateCompletionPolicyRequest;
import com.fap.result.entity.ClassCompletionQuiz;
import com.fap.result.mapper.CourseResultMapper;
import com.fap.result.repository.ClassCompletionQuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CompletionPolicyService {
	private final ClassRepository classRepository;
	private final ClassService classService;
	private final ClassCompletionQuizRepository completionQuizRepository;
	private final QuizRepository quizRepository;
	private final QuizAssignmentRepository quizAssignmentRepository;
	private final AuditLogService auditLogService;
	private final CourseResultMapper courseResultMapper;

	public CompletionPolicyService(
			ClassRepository classRepository,
			ClassService classService,
			ClassCompletionQuizRepository completionQuizRepository,
			QuizRepository quizRepository,
			QuizAssignmentRepository quizAssignmentRepository,
			AuditLogService auditLogService,
			CourseResultMapper courseResultMapper) {
		this.classRepository = classRepository;
		this.classService = classService;
		this.completionQuizRepository = completionQuizRepository;
		this.quizRepository = quizRepository;
		this.quizAssignmentRepository = quizAssignmentRepository;
		this.auditLogService = auditLogService;
		this.courseResultMapper = courseResultMapper;
	}

	@Transactional(readOnly = true)
	public CompletionPolicyResponse getPolicy(Long classId) {
		FapClass fapClass = classRepository.getWithTrainingProgramOrThrow(classId);
		return courseResultMapper.toPolicyResponse(fapClass, completionQuizRepository.findByFapClassIdOrderByIdAsc(classId));
	}

	@Transactional
	public CompletionPolicyResponse updatePolicy(Long classId, UpdateCompletionPolicyRequest request, Long currentUserId) {
		FapClass fapClass = classRepository.getWithTrainingProgramForUpdateOrThrow(classId);
		if (fapClass.getStatus() == ClassStatus.Closed) {
			throw new ConflictException("CLASS_COMPLETION_POLICY_LOCKED", "Closed class completion policy cannot be changed");
		}
		Set<Long> quizIds = new HashSet<>();
		for (CompletionPolicyQuizRequest item : request.requiredQuizzes()) {
			if (!quizIds.add(item.quizId())) {
				throw new BadRequestException("DUPLICATE_REQUIRED_QUIZ", "Required quizzes must be unique");
			}
			if (!quizAssignmentRepository.existsByQuizIdAndFapClassId(item.quizId(), classId)) {
				throw new BadRequestException("QUIZ_NOT_ASSIGNED_TO_CLASS", "Required quiz must be assigned directly to the class");
			}
		}

		LocalDateTime now = LocalDateTime.now();
		completionQuizRepository.deleteByFapClassId(classId);
		completionQuizRepository.flush();
		List<ClassCompletionQuiz> saved = request.requiredQuizzes().stream().map(item -> {
			Quiz quiz = quizRepository.getQuizOrThrow(item.quizId());
			ClassCompletionQuiz policyQuiz = new ClassCompletionQuiz();
			policyQuiz.setFapClass(fapClass);
			policyQuiz.setQuiz(quiz);
			policyQuiz.setPassingScore(item.passingScore());
			policyQuiz.setCreatedAt(now);
			policyQuiz.setUpdatedAt(now);
			policyQuiz.setCreatedBy(currentUserId);
			policyQuiz.setUpdatedBy(currentUserId);
			return completionQuizRepository.save(policyQuiz);
		}).toList();
		classService.updateMinimumAttendanceRate(fapClass, request.minimumAttendanceRate(), currentUserId, now);
		auditLogService.record("UPDATE_COMPLETION_POLICY", "class", classId);
		return courseResultMapper.toPolicyResponse(fapClass, saved);
	}
}
