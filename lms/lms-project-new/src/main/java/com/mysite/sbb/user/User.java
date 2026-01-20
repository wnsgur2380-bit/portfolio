package com.mysite.sbb.user;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.mysite.sbb.board_answer.BoardAnswer;
import com.mysite.sbb.board_question.BoardQuestion;
import com.mysite.sbb.classes.Classes;
import com.mysite.sbb.enrollment.Enrollment;
import com.mysite.sbb.level.Level;
import com.mysite.sbb.quiz_attempt.QuizAttempt;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "site_user") // DB 예약어 'user' 회피를 위해 테이블명 변경 권장
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "uno")
	private Long uno; // 사용자 ID

	@ManyToOne
	@JoinColumn(name = "level_id", nullable = true) // 기본값 : 수강생만 레벨있고 강사, 관리자는 레벨없음
	private Level level; // 레벨 (FK)

	@Column(name = "user_name", nullable = false, length = 100)
	private String userName; // 이름

	@Column(name = "user_id", nullable = false, unique = true, length = 100)
	private String userId; // 아이디

	@Column(unique = true, nullable = false)
	private String email; // 이메일

	@Column(nullable = false)
	private String password; // 비밀번호

	@Enumerated(EnumType.STRING) // DB에는 Enum의 이름(문자열)이 저장됩니다. (예: "LEARNER")
	@Column(length = 20, nullable = false)
	private UserRole role; // 역할 (기본값 설정)

	// 관리자 승인 여부 추가
	@Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
	private boolean approved = false; // 관리자 승인 여부 (기본값 false 미승인)

	@CreationTimestamp // 가입일자는 생성 시 고정
	@Column(name = "user_cdate", nullable = false, updatable = false)
	private LocalDateTime userCdate; // 가입일자

	@Column(name="is_paid", nullable = false)
	private boolean isPaid= false; // true면 VIP(무제한), false면 무료체험(기간제)

	@Column(name = "end_date")
	private LocalDateTime endDate; // 무료체험 종료일

	// 사용자가 푼 퀴즈 결과 목록 (1명의 사용자는 여러 번 퀴즈를 시도할 수 있음)
	// 사용자가 삭제되면 관련된 QuizAttempt도 모두 삭제됨
	@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
	private List<QuizAttempt> quizAttempt;

	// 사용자의 수강 이력 (1명의 사용자는 여러 강의를 수강할 수 있음)
	// 사용자가 삭제되면 해당 사용자의 Enrollment도 모두 삭제됨
	@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
	private List<Enrollment> enrollment;

	// 사용자가 작성한 질문 목록 (질문 게시판)
	// 사용자가 삭제되면 해당 사용자의 질문도 모두 삭제됨
	@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
	private List<BoardQuestion> boardquestion;

	// 사용자가 작성한 답변 목록 (질문 게시판의 답변)
	// 사용자가 삭제되면 해당 사용자의 답변도 모두 삭제됨
	@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
	private List<BoardAnswer> boardanswer;

	// 사용자가 생성한 강의 목록 (해당 사용자가 강사인 경우)
	// 사용자가 삭제되면 해당 사용자가 만든 강의들도 삭제됨
	@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
	private List<Classes> classes;
}