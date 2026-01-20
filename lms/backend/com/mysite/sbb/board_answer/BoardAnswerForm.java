package com.mysite.sbb.board_answer;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardAnswerForm {
	
	private Long boardQuesId;
    private Long uno;
	@NotBlank(message = "답변 내용은 필수 항목입니다.")
	private String answContent;
	
}
