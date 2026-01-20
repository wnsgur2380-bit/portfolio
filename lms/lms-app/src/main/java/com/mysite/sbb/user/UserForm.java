package com.mysite.sbb.user;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserForm {
	
	// PK는 DB에서 회원가입 시 자동 생성 (PK) → 입력받지 않음
    // private Long uno;
	
	@NotBlank(message = "이름은 필수입니다.")
	private String userName;
	
	@NotBlank(message = "아이디는 필수입니다.")
	private String userId;
	
	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "올바른 이메일 형식을 입력해주세요.")
	private String email;
	
	@NotBlank(message = "비밀번호는 필수입니다.")
	@Size(min = 8, max = 16, message = "비밀번호는 8자 이상 16자 이하로 설정해주세요.")
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,16}$",
	// \\d : 0~9까지 / (?=.*) : 해당 값이 반드시 포함되어야함. / {min,max}$ : 최소/최대값 
    message = "비밀번호는 영문 대/소문자, 숫자, 특수문자(!@#$%^&*)를 각각 1개 이상 포함해야 합니다.")
	private String password;
	
	@NotBlank(message = "비밀번호 확인은 필수입니다.") // 1. 비밀번호 확인 필드 추가
	@Size(min = 8, max = 16, message = "비밀번호는 8자 이상 16자 이하로 설정해주세요.")
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,16}$",
	// \\d : 0~9까지 / (?=.*) : 해당 값이 반드시 포함되어야함. / {min,max}$ : 최소/최대값 
    message = "비밀번호는 영문 대/소문자, 숫자, 특수문자(!@#$%^&*)를 각각 1개 이상 포함해야 합니다.")
	private String password1;
	
	@NotBlank
    private String role;
	
	
	@NotNull(message = "결제 여부는 필수 입력값입니다.")
    private Boolean isPaid = false; // 기본값 false로 설정

	
}