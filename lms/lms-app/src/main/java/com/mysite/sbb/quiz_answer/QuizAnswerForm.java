package com.mysite.sbb.quiz_answer;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizAnswerForm {
	
    /**
     * 설명: startAttempt에서 미리 생성된 20개 QuizAnswer 레코드의 고유 ID입니다.
     * 이 ID를 기준으로 어떤 레코드를 업데이트할지 식별합니다.
     */
	@NotNull(message = "답안 ID는 필수입니다.")
    private Long qAnswerId;

    /**
     * 설명: 템플릿 폼(quiz_exam.html)에서 th:field로 바인딩하기 위해 사용합니다.
     */
    @NotNull(message = "문제 ID는 필수입니다.")
    private Long qQuestionId;

    /**
     * 설명: 사용자가 선택한 라디오 버튼의 값 (예: "보기 1의 텍스트")
     */
    private String userAnswer;

}