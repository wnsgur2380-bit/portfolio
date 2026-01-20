package com.mysite.sbb.quiz;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizForm {

	private Long quizId; //퀴즈 번호
	
	@NotNull(message = "레벨 ID는 필수입니다.")
	private Long levelId; //레벨 ID
	
	@NotBlank(message = "퀴즈 제목은 필수입니다.")
	@Size(max = 200, message = "퀴즈 제목은 200자 이내로 입력해주세요.")
	private String quizTitle; //퀴즈 제목
	
	@Min(value = 0, message = "총점은 0 이상이어야 합니다.")
	private int totalScore; //퀴즈 총점
	
	@NotBlank(message = "퀴즈 종류는 필수입니다.")
    @Size(max = 20, message = "퀴즈 종류는 20자 이내로 입력해주세요.")
	private String quizType; //퀴즈 종류

}