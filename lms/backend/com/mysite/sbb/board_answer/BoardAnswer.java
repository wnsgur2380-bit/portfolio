package com.mysite.sbb.board_answer;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.mysite.sbb.board_question.BoardQuestion;
import com.mysite.sbb.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "board_answer")
public class BoardAnswer {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "board_answ_id")
	private Long boardAnswId; // 답변번호
	
	@ManyToOne
	@JoinColumn(name = "board_ques_id", nullable=false)
	private BoardQuestion question; // 질문번호
	
	@ManyToOne
	@JoinColumn(name = "uno", nullable=false)
	private User user; // 회원번호
	
	@Column(name = "answ_content", columnDefinition = "TEXT")
	private String answContent; // 답변내용
	
	@CreationTimestamp // JPA가 생성 시각을 자동 저장
	@Column(name = "answ_date", updatable = false)
	private LocalDateTime answDate; // 답변작성일
}
