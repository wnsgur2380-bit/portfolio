package com.mysite.sbb.user;

import lombok.Getter;

@Getter
public enum UserRole {
	ROLE_ADMIN("ROLE_ADMIN", "관리자"),
	ROLE_INSTRUCTOR("ROLE_INSTRUCTOR", "강사"),
	ROLE_LEARNER("ROLE_LEARNER", "수강자");
	
	private String key;
	private String description;
	
	UserRole(String key, String description) {
        this.key = key;
        this.description = description;
	}
}
