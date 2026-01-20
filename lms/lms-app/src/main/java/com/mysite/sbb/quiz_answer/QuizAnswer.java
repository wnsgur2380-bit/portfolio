package com.mysite.sbb.quiz_answer;

import com.mysite.sbb.quiz_attempt.QuizAttempt;
import com.mysite.sbb.quiz_question.QuizQuestion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class QuizAnswer {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "answer_id")
	private Long answerId; //답안 번호
	
	@ManyToOne
	@JoinColumn(name = "question_id", nullable = false)
	private QuizQuestion qQuestion; // 각 답안은 1개의 문제에 연결
	
	@ManyToOne
	@JoinColumn(name = "attempt_id", nullable = false)
	private QuizAttempt qAttempt; // 각 답안은 1개의 응시 기록에 연결

	@Column(name = "user_answer",columnDefinition = "TEXT")
	private String userAnswer; // 사용자 입력 답

	@Column(name = "is_correct", nullable = false)
    private boolean isCorrect = false; // 정답 여부

}
