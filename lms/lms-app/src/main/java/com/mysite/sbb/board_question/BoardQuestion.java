package com.mysite.sbb.board_question;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.mysite.sbb.board_answer.BoardAnswer;
import com.mysite.sbb.classes.Classes;
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
@Table(name = "board_question")
public class BoardQuestion {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "board_ques_id")
	private Long boardQuesId; // 질문번호
	
	@ManyToOne
	@JoinColumn(name = "class_id", nullable=false)
	private Classes classes; // 강의번호
	
	@ManyToOne
	@JoinColumn(name = "uno", nullable=false)
	private User user; // 회원번호
	
	@Column(length = 255)
	private String title; // 질문제목
	
	@Column(name = "ques_content", columnDefinition = "TEXT")
	private String quesContent; // 질문내용
	
	@CreationTimestamp 
	@Column(name = "ques_date", nullable=false, updatable = false)
	private LocalDateTime quesDate; // 질문작성일
	
	@OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<BoardAnswer> answers = new ArrayList<>(); // 질문 삭제 시, 답변 동시 삭제
}
