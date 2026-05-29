package com.fap.quiz.repository;

import com.fap.quiz.entity.QuizAttempt;
import com.fap.quiz.enums.QuizAttemptStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

	long countByQuizIdAndUserId(Long quizId, Long userId);

	long countByQuizIdAndUserIdAndStatus(Long quizId, Long userId, QuizAttemptStatus status);

	@EntityGraph(attributePaths = {"quiz", "user"})
	Optional<QuizAttempt> findByIdAndUserId(Long id, Long userId);

	@EntityGraph(attributePaths = {"quiz", "user"})
	Optional<QuizAttempt> findFirstByQuizIdAndUserIdOrderByIdDesc(Long quizId, Long userId);

	@EntityGraph(attributePaths = {"quiz", "user"})
	Optional<QuizAttempt> findFirstByQuizIdAndUserIdAndStatusOrderByIdDesc(
			Long quizId,
			Long userId,
			QuizAttemptStatus status);
}
