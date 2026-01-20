package com.mysite.sbb.quiz_question;

import com.mysite.sbb.quiz.Quiz;
import com.mysite.sbb.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "quiz_question")
public class QuizQuestion {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "question_id")
	private Long questionId; //문제 번호
	
	@ManyToOne
	@JoinColumn(name = "quiz_id", nullable = false)
	private Quiz quiz; // Quiz FK
	
	@Column(name = "quiz_content", columnDefinition = "TEXT", nullable = false)
	private String quizContent; // 문제 내용
	
	// --- 객관식 보기 필드 추가 ---
	@Column(name = "option1", columnDefinition = "TEXT")
	private String option1;

	@Column(name = "option2", columnDefinition = "TEXT")
	private String option2;

	@Column(name = "option3", columnDefinition = "TEXT")
	private String option3;

	@Column(name = "option4", columnDefinition = "TEXT")
	private String option4;
	// -----------------------
	
	@Column(name = "score", nullable = false)
	private Integer score = 5; // 기본 5점

	@Column(name = "correct_answer", columnDefinition = "TEXT", nullable = false)
	private String correctAnswer;
	
	@ManyToOne
	@JoinColumn(name = "author_uno")
    private User author;

}