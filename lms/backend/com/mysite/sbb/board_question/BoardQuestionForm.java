package com.mysite.sbb.board_question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardQuestionForm {
	
    @NotNull(message = "질문할 강의를 선택해주세요.")
    private Long classId;
	
	@NotBlank(message = "제목은 필수 항목입니다.")
	@Size(max=255)
	private String title;
	
	@NotBlank(message = "내용은 필수 항목입니다.")
	private String quesContent;
}
