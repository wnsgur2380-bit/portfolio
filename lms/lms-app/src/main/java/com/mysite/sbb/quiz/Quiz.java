package com.mysite.sbb.quiz;

import java.util.List;

import com.mysite.sbb.level.Level;
import com.mysite.sbb.quiz_attempt.QuizAttempt;
import com.mysite.sbb.quiz_question.QuizQuestion;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Quiz {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "quiz_id")
	private Long quizId; //퀴즈 ID
	
	@ManyToOne
	@JoinColumn(name = "level_id") // 레벨 테스트는 null
	private Level level; // Level FK 연결 (승급 테스트용)
	
	@Column(name = "quiz_title", length = 200, nullable = false)
	private String quizTitle;
	
	@Column(name = "total_score") // 기본값 100점으로 null 허용
	private int totalScore = 100; // 총점은 100점으로 기본 설정
	
	@Column(name = "quiz_type", length = 50, nullable = false)
	private String quizType; // "LEVEL_TEST", "PROMOTION_TEST_BEGINNER", "PROMOTION_TEST_INTERMEDIATE"
	
	// Quiz와 Quiz_question 간의 관계 설정 (1:N)
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizQuestion> quizQuestions;
	
    // Quiz와 Quiz_attempt 간의 관계 설정 (1:N)
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAttempt> quizAttempts;
}