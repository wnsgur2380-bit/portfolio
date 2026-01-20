package com.mysite.sbb.level;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LevelForm {
	
	@NotBlank(message = "레벨 이름은 필수 입력값입니다.")
    @Size(max = 20, message = "레벨 이름은 20자 이하로 입력해주세요.")
    private String levelName;

}
