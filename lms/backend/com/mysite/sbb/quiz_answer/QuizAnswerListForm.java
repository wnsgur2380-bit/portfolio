package com.mysite.sbb.quiz_answer;

import java.util.List;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizAnswerListForm {
	
	// 템플릿의 폼에서 전송된 20개의 답안 목록을 담을 리스트
	@Valid
	private List<QuizAnswerForm> answerForms;
}
