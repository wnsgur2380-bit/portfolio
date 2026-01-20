package com.mysite.sbb.quiz_attempt;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mysite.sbb.quiz.Quiz;
import com.mysite.sbb.user.User;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long>{
	// 특정 회원(uno)의 응시 기록 조회
    List<QuizAttempt> findByUserUno(Long uno);

    // 특정 퀴즈(quizId)의 응시 기록 조회
    List<QuizAttempt> findByQuizQuizId(Long quizId);
    
    // 특정 사용자와 특정 퀴즈에 대한 가장 최근 응시 기록 조회 (QuizAttemptService에서 사용)
    Optional<QuizAttempt> findFirstByUserAndQuizOrderByAttemptedCdateDesc(User user, Quiz quiz);
    
    // 특정 사용자와 특정 퀴즈 타입으로 응시 기록 존재 여부 확인 (레벨테스트 중복 응시 방지)
    boolean existsByUserAndQuiz_QuizType(User user, String quizType);
}