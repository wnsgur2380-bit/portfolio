package com.mysite.sbb.quiz_question;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long>{
	
	List<QuizQuestion> findByQuiz_QuizId(Long quizId);
	
	// 퀴즈 ID로 문제를 찾되, 페이징을 적용하는 메소드
	Page<QuizQuestion> findByQuizQuizId(Long quizId, Pageable pageable);
	
	// 특정 퀴즈(Quiz)에 속한 문제들 중 20개를 랜덤으로 추출합니다.
	@Query(value = "SELECT * FROM quiz_question WHERE quiz_id = :quizId ORDER BY RAND() LIMIT 20", nativeQuery = true)
	List<QuizQuestion> findRandom20QuestionsByQuizId(@Param("quizId") Long quizId);
	
	@Query("select q from QuizQuestion q left join q.author a "
			+ "where q.quiz.quizId = :quizId "
			+ "and (lower(q.quizContent) like lower(concat('%', :kw, '%')) "
			+ "or (a is not null and lower(a.userName) like lower(concat('%', :kw, '%'))))")
	Page<QuizQuestion> findAllByKeyword(@Param("quizId") Long quizId, @Param("kw") String kw, Pageable pageable);
	
}
