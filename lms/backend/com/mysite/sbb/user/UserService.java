package com.mysite.sbb.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mysite.sbb.DataNotFoundException;
import com.mysite.sbb.level.Level;
import com.mysite.sbb.level.LevelRepository;
import com.mysite.sbb.quiz.Quiz;
import com.mysite.sbb.quiz.QuizRepository;
import com.mysite.sbb.quiz_attempt.QuizAttemptService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserService {

	private final UserRepository userr;
	private final PasswordEncoder passworde; // 생성자 주입 / 비밀번호 암호화용
	private final LevelRepository levelr; // 기본 레벨 연결용 (nullable = false 대응)
	private final QuizRepository quizr;

	@Autowired
	@Lazy // 순환 참조 문제를 해결하기 위해 @Lazy 사용
	private QuizAttemptService qaService;
	
	// - 회원가입 (UserForm 기반 수강생, 강사, 관리자 포함) 공통
		public User signup(UserForm form) {

			// 1. 중복 체크 (아이디/이메일)
			if (userr.findByUserId(form.getUserId()).isPresent()) {
				throw new DataIntegrityViolationException("이미 사용 중인 아이디입니다.");
			}
			if (userr.findByEmail(form.getEmail()).isPresent()) {
				throw new DataIntegrityViolationException("이미 등록된 이메일입니다.");
			}

			// 2. DTO → 엔티티 변환
			User user = new User();
			user.setUserName(form.getUserName());
			user.setUserId(form.getUserId());
			user.setEmail(form.getEmail());
			user.setPassword(passworde.encode(form.getPassword())); // 암호화
			user.setEndDate(null); // 가입 시점엔 아직 종료일 없음

			// 3. 구분 지정 수정!!
			if ("ROLE_INSTRUCTOR".equals(form.getRole())) {
				user.setRole(UserRole.ROLE_INSTRUCTOR);
			} else if ("ROLE_ADMIN".equals(form.getRole())) {
		        user.setRole(UserRole.ROLE_ADMIN);
			}  else {
				user.setRole(UserRole.ROLE_LEARNER); // 기본값
				// ★ [핵심] 수강생은 가입일로부터 7일간 무료 체험
	            user.setEndDate(LocalDateTime.now().plusDays(7));
	            user.setPaid(false); // 기본은 미결제 상태
			}
				
			// 3. 4. 수강생만 설정 (예: ID가 1L인 레벨을 '초급'으로 가정)
			if (user.getRole() == UserRole.ROLE_LEARNER) {
			Level defaultLevel = levelr.findById(1L)
					.orElseThrow(() -> new DataNotFoundException("기본 레벨(ID: 1)을 찾을 수 없습니다."));
			user.setLevel(defaultLevel);
			} else {
		        user.setLevel(null); // 강사, 관리자일 경우 level 없이 저장
		    }

			// 5. DB 저장
			User savedUser = userr.save(user); // 저장된 User 객체 반환받기

			// ---- 6. 회원가입 후 수강생인 경우에만 레벨 테스트 자동 시작 ---- //
			// 1. 레벨 테스트 퀴즈 찾기 (quiztype 기준)
			if (savedUser.getRole() == UserRole.ROLE_LEARNER) {
				Optional<Quiz> levelTestQuizOpt = quizr.findByQuizType("LEVEL_TEST");

				if (levelTestQuizOpt.isPresent()) {
					Quiz levelTestQuiz = levelTestQuizOpt.get();
					// 2. QuizAttemptService를 사용하여 응시 기록 생성
					try {
						qaService.startAttempt(levelTestQuiz, savedUser);
					} catch (Exception e) {
						System.err.println("회원가입 후 레벨 테스트 시작 오류: " + e.getMessage());
					}
				} else {
					System.err.println("회원가입 후 레벨 테스트를 찾을 수 없습니다.");
				}
			}
			return savedUser;
		}

	
	// - (관리자) 강사 승인 메서드
		public void approveInstructor(Long uno) {
			User user = getUser(uno);
			if (user.getRole() != UserRole.ROLE_INSTRUCTOR) {
				throw new IllegalArgumentException("강사만 승인할 수 있습니다.");
			}
			user.setApproved(true);
			userr.save(user);
		}
		
	// - (관리자용) 회원정보수정
	public void updateUser(Long uno, UserForm userForm, String newPassword) {
		User user = userr.findById(uno).orElseThrow(() -> new DataNotFoundException("사용자를 찾을 수 없습니다. id=" + uno));

		String newEmail = userForm.getEmail();
		if (!user.getEmail().equals(newEmail)) {
			// 이메일이 변경되었으면, 다른 사용자가 이 이메일을 쓰고 있는지 확인
			Optional<User> existingUser = userr.findByEmail(newEmail);
			if (existingUser.isPresent() && !existingUser.get().getUno().equals(user.getUno())) {
				// 다른 사용자가 이미 사용중일 때
				throw new DataIntegrityViolationException("이미 사용 중인 이메일입니다.");
			}
			user.setEmail(newEmail);
		}
		// 이메일이 같다면 아무것도 안함
		user.setUserName(userForm.getUserName());
		// UserForm에서 String으로 받은 role을 UserRole Enum 타입으로 변환
		user.setRole(UserRole.valueOf(userForm.getRole()));

		if (newPassword != null && !newPassword.isEmpty()) {
			user.setPassword(passworde.encode(newPassword));
		}
		userr.save(user);
	}

	// - (수강생, 강사) 내 정보 수정 메서드
	public void modifyMyInfo(User user, String newPassword, String newEmail) {
		// 1. 이메일 변경
		if (!user.getEmail().equals(newEmail)) {
			// 이메일이 변경된 경우에만 중복 검사
			Optional<User> existingUser = userr.findByEmail(newEmail);
			if (existingUser.isPresent() && !existingUser.get().getUno().equals(user.getUno())) {
				// 이메일이 존재하는데, 그게 내(user)가 아니면 -> 중복
				throw new DataIntegrityViolationException("이미 사용 중인 이메일입니다.");
			}
			user.setEmail(newEmail);
		}

		// 비밀번호 변경
		if (newPassword != null && !newPassword.isEmpty()) {
			user.setPassword(passworde.encode(newPassword));
		}

		// 저장
		userr.save(user);
	}

	
	// - 현재 로그인된 사용자 가져오기(조회)
		public User getCurrentUser() {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication == null || !authentication.isAuthenticated()) {
				throw new IllegalStateException("인증된 사용자가 없습니다.");
			}

			String username = authentication.getName();
			return userr.findByUserId(username).orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
		}
		
	// - 기타 기능
	// 회원 관련 CRUD
	// 전체 회원 조회
	public List<User> getAllUsers() {
		return userr.findAll();
	}

	// 단일 조회 (uno 기준)
	public User getUser(Long uno) {
		return userr.findById(uno).orElseThrow(() -> new DataNotFoundException("사용자를 찾을 수 없습니다. ID: " + uno));
	}

	// 단일 조회 (userId 기준) - Controller에서 Principal 대신 사용할 경우 필요
	public User getUser(String userId) {
		return userr.findByUserId(userId)
				.orElseThrow(() -> new DataNotFoundException("사용자를 찾을 수 없습니다. User ID: " + userId));
	}

	// 회원 삭제
	public void deleteUser(Long uno) {
		userr.deleteById(uno);
	}
	
	
	// 랜덤 강사 조회 (4명) 메인페이지에서
	public List<User> getRandomInstructors(int limit) {
		Pageable pageable = PageRequest.of(0, limit);
		return userr.findRandomByRole(UserRole.ROLE_INSTRUCTOR, pageable).getContent();
	}

	// save 편의 메서드
	public void save(User user) {
		userr.save(user);
	}
	
	
	// 1. user_list.html(전체 회원용)
	// 검색 - 구분 필터 + 키워드 검색 (이름, 아이디, 이메일에 대해 검색 가능)
	// roleFilter: "ALL"이면 전체, "ROLE_LEARNER", "ROLE_INSTRUCTOR" 등으로 구분
	// keyword: 이름, 아이디, 이메일 중 일부 포함 시 검색
	public Page<User> getFilteredUsers(int page, String roleFilter, String keyword) {
		// 공백 처리
	    // "ALL" 또는 null/공백 → 기본값 : 전체 유저
	    if (roleFilter == null || roleFilter.isBlank()) {
	    	roleFilter = "ALL"; // 기본값: 전체 회원 조회
	    }
	    if (keyword == null) {
	        keyword = ""; // 검색어가 없으면 전체 대상
	    }
	    
	    // 최신순으로 10개씩 조회
	    Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "userCdate"));

	    return userr.findByKeywordAndRole(keyword.toLowerCase(), roleFilter, pageable);
	}
	
	// 2. user_list_unapproved.html(비승인 강사용)
	// 승인되지 않은 강사 전체 조회 (approved = false && role = ROLE_INSTRUCTOR)
    public Page<User> getPendingInstructors(int page, String keyword) {
    	Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "userCdate"));
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return userr.findByRoleAndApprovedFalse(UserRole.ROLE_INSTRUCTOR, pageable);
        } else {
            return userr.searchUnapprovedInstructors(UserRole.ROLE_INSTRUCTOR, keyword.trim().toLowerCase(), pageable);
        }
    }

    // 3. user_list_dormant.html (휴면 회원용) - 페이징 적용
    public Page<User> getDormantUsers(int page, String keyword) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "userCdate"));
        LocalDateTime threshold = LocalDateTime.now().minusYears(1); // 1년 이상 미활동
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return userr.findDormantUsers(threshold, pageable);
        } else {
            return userr.findDormantUsersByKeyword(threshold, keyword.trim().toLowerCase(), pageable);
        }
    }
    
 // [추가] 사용자가 강의를 볼 수 있는 상태인지 확인하는 헬퍼 메서드
    public boolean canAccessCourse(User user) {
        // 1. 관리자나 강사는 프리패스
        if (user.getRole() == UserRole.ROLE_ADMIN || user.getRole() == UserRole.ROLE_INSTRUCTOR) {
            return true;
        }
        // 2. 결제한 VIP 회원이면 프리패스
        if (user.isPaid()) {
            return true;
        }
        // 3. 미결제 회원이면 날짜 확인 (현재 시간이 종료일 이전이어야 함)
        if (user.getEndDate() != null && LocalDateTime.now().isBefore(user.getEndDate())) {
            return true;
        }
        
        return false; // 그 외에는 접근 불가
    }

    // [추가] 결제 처리 (VIP 등업) 메서드
    public void upgradeToVip(User user) {
        user.setPaid(true);
        user.setEndDate(null); // VIP는 기한 없음 (혹은 결제일 + 30일 등으로 설정 가능)
        userr.save(user);
    }
}