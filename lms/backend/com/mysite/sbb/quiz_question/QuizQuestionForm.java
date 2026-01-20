package com.mysite.sbb.quiz_question;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class QuizQuestionForm {
	
	@NotNull(message = "퀴즈 ID는 필수입니다.")
	private Long quizId;
	
	@NotNull(message = "문제 내용을 입력해주세요.")
	private String quizContent;
	
	// --- 객관식 보기 필드 추가 ---
	@NotNull(message = "보기 1 내용을 입력해주세요.")
	private String option1;

	@NotNull(message = "보기 2 내용을 입력해주세요.")
	private String option2;

	@NotNull(message = "보기 3 내용을 입력해주세요.")
	private String option3;

	@NotNull(message = "보기 4 내용을 입력해주세요.")
	private String option4;
	
	@NotNull(message = "정답을 입력해주세요.")
    private String correctAnswer;

    @NotNull(message = "문제 점수를 입력해주세요.")
    private Integer score = 5; // 기본 5점
}