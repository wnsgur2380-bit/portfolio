package com.mysite.sbb.quiz;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
	
	// quizType으로 퀴즈 조회 (UserService에서 사용)
	Optional<Quiz> findByQuizType(String quizType); // quizType으로 퀴즈 조회
}