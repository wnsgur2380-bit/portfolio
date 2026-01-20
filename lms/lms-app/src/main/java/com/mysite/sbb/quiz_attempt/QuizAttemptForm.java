package com.mysite.sbb.quiz_attempt;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizAttemptForm {
	
	@NotNull(message = "퀴즈 ID는 필수입니다.")
    private Long quizId;

    @NotNull(message = "회원 번호는 필수입니다.")
    private Long uno; // (uno → userId로 정확하게)

	// 선택적: 점수 초기화 (생성 시 0점으로 시작)
    private Integer score = 0;
}