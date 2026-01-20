package com.mysite.sbb.classes;

import java.time.LocalDateTime;

import org.hibernate.annotations.CurrentTimestamp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClassesForm {

	private Long classesId; //강의번호 (수정 시만 사용)
	
	private Long uno; //회원번호(강사) //사용자 ID?
	
	@NotNull(message = "레벨은 필수입니다.")
	private Long levelId; //레벨ID
	
	@NotBlank(message = "제목은 필수입니다.")
	private String title; //강의 제목
	
	@NotBlank(message = "내용은 필수입니다.")
	private String classesContent; //강의 내용
	
	// 이미지 URL 필드 추가
	private String classesImg; // 강의 이미지 URL
		
	// 비디오 URL 필드 추가
	private String classesVideo; // 강의 비디오 URL
	
	@CurrentTimestamp
	private LocalDateTime classesCdate; // 등록일

	
}