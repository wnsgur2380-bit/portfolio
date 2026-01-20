package com.mysite.sbb.quiz_attempt;

import java.time.LocalDateTime;
import java.util.List;

import com.mysite.sbb.quiz.Quiz;
import com.mysite.sbb.quiz_answer.QuizAnswer;
import com.mysite.sbb.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "quiz_attempt")
public class QuizAttempt {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "attempt_id")
	private Long attemptId; //응시 번호
	
	// Quiz FK (N:1)
	@ManyToOne
	@JoinColumn(name="quiz_id", nullable = false)
	private Quiz quiz;
	
	// User FK (N:1)
	@ManyToOne
    @JoinColumn(name = "uno", nullable = false)
    private User user;
	
	// QuizAnswer FK (1:N)
    @OneToMany(mappedBy = "qAttempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAnswer> qAnswers; // 응시한 답안 목록
    
	@Column(name = "score", nullable = false)
	private Integer score = 0;
	
	@Column(name = "attempted_cdate", nullable = false)
	private LocalDateTime attemptedCdate = LocalDateTime.now();
	
}
