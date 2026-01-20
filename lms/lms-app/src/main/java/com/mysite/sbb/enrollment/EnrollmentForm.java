package com.mysite.sbb.enrollment;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnrollmentForm {

	// 클릭된 강의의 ID (백엔드가 hidden input으로 넘김)
    @NotNull(message = "선택된 강의가 없습니다.")
    private Long classesId; // 클릭된 강의(FK)
	
	
	
	
}