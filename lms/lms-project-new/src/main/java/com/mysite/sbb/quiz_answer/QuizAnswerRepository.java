package com.mysite.sbb.quiz_answer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mysite.sbb.quiz_attempt.QuizAttempt;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long>{
	List<QuizAnswer> findByqAttempt(QuizAttempt qAttempt);
}
