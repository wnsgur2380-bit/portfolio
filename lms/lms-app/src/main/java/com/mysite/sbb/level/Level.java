package com.mysite.sbb.level;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name="level")
public class Level {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="level_id")
	private Long levelId;
	
	@Column(name="level_name", length = 20, nullable = false, unique = true)
	private String levelName; // 초급 / 중급 / 고급
	
}
