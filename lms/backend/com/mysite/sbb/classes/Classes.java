package com.mysite.sbb.classes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.mysite.sbb.board_question.BoardQuestion;
import com.mysite.sbb.enrollment.Enrollment;
import com.mysite.sbb.level.Level;
import com.mysite.sbb.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name="classes")
public class Classes {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="classes_id")
	private Long classesId; //강의번호
	
	@ManyToOne
	@JoinColumn(name="uno", nullable = false)
	private User user; //강사 (User FK)
	
	@ManyToOne
	@JoinColumn(name="level_id",nullable = false)
	private Level level; //난이도 (Level FK)
	
	@Column(name = "title", nullable = false)
	private String title;	//강의제목
	
	@Column(name = "classes_content", columnDefinition = "TEXT")
	private String classesContent; //강의내용
	
	@CreationTimestamp
	@Column(name = "created_date", nullable = false, updatable = false)
	private LocalDateTime classesCdate; //등록일
	
	@Column(name = "classes_img")
	private String classesImg;  //강의이미지
	
	@Column(name = "classes_video")
	private String classesVideo; //강의영상

	
	// cascade = CascadeType.ALL 추가
	@OneToMany(mappedBy = "classes", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<BoardQuestion> questions = new ArrayList<>();
		
	// Enrollment 관련 설정 - cascade 확인
	@OneToMany(mappedBy = "classes", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Enrollment> enrollments = new ArrayList<>();
	
}