package com.fap.quiz.service;

import com.fap.clazz.entity.FapClass;
import com.fap.clazz.repository.ClassRepository;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.quiz.dto.CreateQuizAssignmentRequest;
import com.fap.quiz.dto.QuizAssignmentResponse;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.entity.QuizAssignment;
import com.fap.quiz.enums.QuizStatus;
import com.fap.quiz.mapper.QuizAssignmentMapper;
import com.fap.quiz.repository.QuizAssignmentRepository;
import com.fap.quiz.repository.QuizRepository;
import com.fap.training.entity.TrainingSession;
import com.fap.training.repository.TrainingSessionRepository;
import com.fap.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class QuizAssignmentService {

	private final QuizRepository quizRepository;
	private final QuizAssignmentRepository quizAssignmentRepository;
	private final ClassRepository classRepository;
	private final TrainingSessionRepository trainingSessionRepository;
	private final UserRepository userRepository;
	private final QuizAssignmentMapper quizAssignmentMapper;
	private final AuditLogService auditLogService;

	public QuizAssignmentService(
			QuizRepository quizRepository,
			QuizAssignmentRepository quizAssignmentRepository,
			ClassRepository classRepository,
			TrainingSessionRepository trainingSessionRepository,
			UserRepository userRepository,
			QuizAssignmentMapper quizAssignmentMapper,
			AuditLogService auditLogService) {
		this.quizRepository = quizRepository;
		this.quizAssignmentRepository = quizAssignmentRepository;
		this.classRepository = classRepository;
		this.trainingSessionRepository = trainingSessionRepository;
		this.userRepository = userRepository;
		this.quizAssignmentMapper = quizAssignmentMapper;
		this.auditLogService = auditLogService;
	}

	@Transactional(readOnly = true)
	public List<QuizAssignmentResponse> list(Long quizId) {
		ensureQuizExists(quizId);
		return quizAssignmentRepository.findByQuizIdOrderByIdAsc(quizId).stream()
				.map(quizAssignmentMapper::toResponse)
				.toList();
	}

	@Transactional
	public QuizAssignmentResponse assign(Long quizId, CreateQuizAssignmentRequest request, Long currentUserId) {
		Quiz quiz = findQuiz(quizId);
		ensurePublished(quiz);
		validateScope(request);

		QuizAssignment assignment = new QuizAssignment();
		assignment.setQuiz(quiz);
		assignment.setAssignedBy(userRepository.getReferenceById(currentUserId));
		assignment.setAssignedAt(LocalDateTime.now());

		if (request.classId() != null) {
			assignClassScope(quizId, request.classId(), assignment);
		} else {
			assignTrainingSessionScope(quizId, request.trainingSessionId(), assignment);
		}

		QuizAssignment saved = quizAssignmentRepository.save(assignment);
		auditLogService.record("CREATE_QUIZ_ASSIGNMENT", "quiz_assignment", saved.getId());
		return quizAssignmentMapper.toResponse(saved);
	}

	@Transactional
	public void delete(Long quizId, Long assignmentId) {
		QuizAssignment assignment = quizAssignmentRepository.findByQuizIdAndId(quizId, assignmentId)
				.orElseThrow(() -> new NotFoundException("Quiz assignment not found"));
		ensurePublished(assignment.getQuiz());
		quizAssignmentRepository.delete(assignment);
		auditLogService.record("DELETE_QUIZ_ASSIGNMENT", "quiz_assignment", assignment.getId());
	}

	private void assignClassScope(Long quizId, Long classId, QuizAssignment assignment) {
		if (quizAssignmentRepository.existsByQuizIdAndFapClassId(quizId, classId)) {
			throw new ConflictException("DUPLICATE_QUIZ_CLASS_ASSIGNMENT", "Quiz is already assigned to this class");
		}
		FapClass fapClass = classRepository.findById(classId)
				.orElseThrow(() -> new NotFoundException("Class not found"));
		assignment.setFapClass(fapClass);
	}

	private void assignTrainingSessionScope(Long quizId, Long trainingSessionId, QuizAssignment assignment) {
		if (quizAssignmentRepository.existsByQuizIdAndTrainingSessionId(quizId, trainingSessionId)) {
			throw new ConflictException("DUPLICATE_QUIZ_SESSION_ASSIGNMENT", "Quiz is already assigned to this training session");
		}
		TrainingSession trainingSession = trainingSessionRepository.findById(trainingSessionId)
				.orElseThrow(() -> new NotFoundException("Training session not found"));
		assignment.setTrainingSession(trainingSession);
	}

	private void validateScope(CreateQuizAssignmentRequest request) {
		boolean hasClass = request.classId() != null;
		boolean hasSession = request.trainingSessionId() != null;
		if (hasClass == hasSession) {
			throw new BadRequestException("INVALID_QUIZ_ASSIGNMENT_SCOPE", "Provide exactly one of classId or trainingSessionId");
		}
	}

	private Quiz findQuiz(Long quizId) {
		return quizRepository.findById(quizId)
				.orElseThrow(() -> new NotFoundException("Quiz not found"));
	}

	private void ensureQuizExists(Long quizId) {
		if (!quizRepository.existsById(quizId)) {
			throw new NotFoundException("Quiz not found");
		}
	}

	private void ensurePublished(Quiz quiz) {
		if (quiz.getStatus() != QuizStatus.Published) {
			throw new ConflictException("QUIZ_NOT_ASSIGNABLE", "Only published quiz can be assigned");
		}
	}
}
