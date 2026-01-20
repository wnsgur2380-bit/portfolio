package com.mysite.sbb.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserModifyForm {
	
	@NotEmpty(message = "현재 비밀번호를 입력해주세요.")
	private String currentPassword;
	
	// 새 비밀번호 = 선택 입력 (규칙은 컨트롤러에서 직접 검사)
    private String newPassword1;
	
	private String newPassword2;
	
	@NotEmpty(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	private String email;
}