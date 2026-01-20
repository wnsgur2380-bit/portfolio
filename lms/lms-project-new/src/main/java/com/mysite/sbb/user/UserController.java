package com.mysite.sbb.user;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mysite.sbb.enrollment.EnrollmentService;
import com.mysite.sbb.quiz_attempt.QuizAttemptService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller // 뷰 렌더링 + API 병행 가능
@RequestMapping("/user/")
public class UserController {

	private final UserService users;
	private final EnrollmentService enrollments;
	private final PasswordEncoder passwordEncoder;

	@Autowired
	@Lazy
	private QuizAttemptService qaService;

	// 로그인 페이지 (오류 메시지 전달 포함)
	@GetMapping("/login")
	public String loginPage(@RequestParam(value = "error", required = false) String error, Model model,
			HttpSession session) {

		// SecurityConfig에서 저장된 세션 메시지 꺼내오기
		String errorMsg = (String) session.getAttribute("errorMsg");

		if (errorMsg != null) { // 로그인 시 강사 계정이 approved=false면 → UserSecurityService에서 RuntimeException 발생
			// UserController가 그 메시지 받아서 → model.addAttribute("msg", ...) 전달
			model.addAttribute("errorMsg", errorMsg); // 스프링 시큐리티가 /user/login?error=관리자 승인 시 로그인 및 이용 가능합니다.로 리다이렉트
			session.removeAttribute("errorMsg");
		} else if (error != null) {
			model.addAttribute("errorMsg", "아이디 또는 비밀번호를 확인하세요.");
		}

		return "loginform"; 
	}

	// 회원가입 페이지
	@GetMapping("/signup")
	public String signupForm(Model model, UserForm userForm) {
		return "signupform"; // templates/signupForm.html
	}

	// 회원가입 폼 (API)
	@PostMapping("/signup")
	public String signup(@Valid UserForm form, BindingResult bindingResult, Model model,
			RedirectAttributes redirectAttributes) {
		// 2. @ResponseBody 제거, BindingResult 추가, 반환타입 String

		// 3. 폼 유효성 검사 (예: @NotBlank)
		if (bindingResult.hasErrors()) {
			return "signupform"; // 유효성 검사 실패 시, 다시 회원가입 폼을 보여줌
		}

		// 4. 비밀번호 일치 검증
		if (!form.getPassword().equals(form.getPassword1())) {
			// bindingResult에 오류 추가 (필드명, 에러코드, 기본메시지)
			bindingResult.rejectValue("password1", "passwordInCorrect", "비밀번호 확인이 일치하지 않습니다.");
			return "signupform"; // 오류와 함께 다시 회원가입 폼을 보여줌
		}

		try {
			// 5. 회원가입 실행
			User savedUser = users.signup(form); // 저장된 User 객체 받기

			// [수정] 역할(Role)에 따라 분기 처리
			if (savedUser.getRole() == UserRole.ROLE_INSTRUCTOR) {

				// [강사] : 로그인 페이지로 리다이렉트 (관리자 승인 대기 메시지)
				String message = "강사 회원가입이 완료되었습니다. 관리자 승인 후 로그인해주세요.";
				redirectAttributes.addFlashAttribute("msg", message);
				return "redirect:/user/login";

			} else if (savedUser.getRole() == UserRole.ROLE_LEARNER) { 
				String message = "회원가입이 완료되었습니다. 로그인 후 레벨 테스트를 진행해주세요.";
				redirectAttributes.addFlashAttribute("msg", message);
				
				// [신규] 로그인 페이지로 리다이렉트
				return "redirect:/user/login";
			} else {
				// 혹시 모를 제3의 역할 (예: Admin)
				redirectAttributes.addFlashAttribute("msg", "회원가입이 완료되었습니다. 로그인해주세요.");
				return "redirect:/user/login";
			}

		} catch (DataIntegrityViolationException e) {
			// 6. 중복 아이디/이메일 예외 처리
			bindingResult.reject("signupFailed", "이미 등록된 사용자입니다.");
			return "signupform"; // 오류와 함께 다시 회원가입 폼을 보여줌

		} catch (Exception e) {
			// 7. 그 외 일반적인 예외 처리
			bindingResult.reject("signupFailed", e.getMessage());
			return "signupform"; // 오류와 함께 다시 회원가입 폼을 보여줌
		}
	}


	// 전체 회원 목록
	// 회원관리페이지이동 (역할 + 이름/아이디 검색)
	@GetMapping("/list")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public String adminUserList(Model model,
			@RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "roleSearch", required = false) String roleFilter,
            @RequestParam(value = "keyword", required = false) String keyword) {

		Page<User> paging = users.getFilteredUsers(page, roleFilter, keyword);

		model.addAttribute("paging", paging);
		model.addAttribute("roleSearch", roleFilter);
		model.addAttribute("keyword", keyword);
		model.addAttribute("currentURI", "/user/list");
		model.addAttribute("activeMenu", "user-list");

		return "user_list";
	}

	// 비승인 강사 목록
	@GetMapping("/list/unapproved")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public String listPendingInstructors(Model model,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "keyword", required = false) String keyword) {

		Page<User> paging = users.getPendingInstructors(page, keyword);

		model.addAttribute("paging", paging);
		model.addAttribute("keyword", keyword);
		model.addAttribute("currentURI", "/user/list/unapproved"); // 중요 : URI 기준 맞춤
		model.addAttribute("activeMenu", "user-unapproved");
		return "user_list_unapproved";
	}

	// 휴면 회원 목록
	@GetMapping("/list/dormant")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public String showDormantUsers(Model model,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "keyword", required = false) String keyword) {
		
		Page<User> paging = users.getDormantUsers(page, keyword);

        model.addAttribute("paging", paging);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentURI", "/user/list/dormant");
        model.addAttribute("activeMenu", "user-dormant");
        
        return "user_list_dormant";
	}

	// (관리자용) 회원수정 폼
	@GetMapping("/modify/{uno}")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public String modifyForm(@PathVariable("uno") Long uno, Model model, UserForm userForm) {
		User user = users.getUser(uno); // 사용자 정보 가져오기

		userForm.setUserId(user.getUserId());
		userForm.setUserName(user.getUserName());
		userForm.setEmail(user.getEmail());
		userForm.setRole(user.getRole().getKey());

		model.addAttribute("userForm", userForm);
		model.addAttribute("uno", uno);
		model.addAttribute("user",user);

		return "user_mypage_modifyform_admin"; // 관리자용 템플릿
	}

	// (관리자용) 회원수정 처리
	@PostMapping("/modify/{uno}")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public String modifyUser(@PathVariable("uno") Long uno, @ModelAttribute("userForm") UserForm userForm,
			BindingResult bindingResult, Model model) {

		User userToModify = users.getUser(uno); // 원본 유저 정보
		model.addAttribute("user", userToModify);
		
		// --- 수동 유효성 검사 (이름, 이메일 필수) ---
		if (userForm.getUserName() == null || userForm.getUserName().isBlank()) {
			bindingResult.rejectValue("userName", "required", "이름은 필수항목입니다.");
		}
		if (userForm.getEmail() == null || userForm.getEmail().isBlank()) {
			bindingResult.rejectValue("email", "required", "이메일은 필수항목입니다.");
		}
		
		// 1. DTO에서 비밀번호 필드를 가져옵니다.
		String newPassword = userForm.getPassword();
		String newPasswordConfirm = userForm.getPassword1();
		
		boolean changePassword = false;
		
		// 비밀번호 필드가 비어있지 않을 때만 검증
		if (newPassword != null && !newPassword.trim().isEmpty()) {
			changePassword = true;
			
			// 2. 새 비밀번호 2개가 일치하는지 확인
			if (!newPassword.equals(newPasswordConfirm)) {
				// 3. 글로벌 오류 -> 필드 오류로 변경
				bindingResult.rejectValue("password1", "passwordInCorrect", "새 비밀번호 확인이 일치하지 않습니다.");
			}

			// 4. 비밀번호 패턴 검사 (UserForm의 @Pattern과 동일)
			String pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,16}$";
			if (!newPassword.matches(pattern)) {
				// 5. 글로벌 오류 -> 필드 오류로 변경
				bindingResult.rejectValue("password", "passwordPattern", "비밀번호는 8~16자, 대/소문자, 숫자, 특수문자를 포함해야 합니다.");
			}
		}
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("uno", uno);
			return "user_mypage_modifyform_admin";
		}

		try {
			// userForm 객체(formData)와 newPassword를 전달
			users.updateUser(uno, userForm, changePassword ? newPassword : null);

		} catch (DataIntegrityViolationException e) {
			bindingResult.reject("modifyFailed", "이미 사용 중인 이메일이거나 다른 제약 조건 위반입니다.");
			model.addAttribute("uno", uno);
			userForm.setUserId(userToModify.getUserId());
			return "user_mypage_modifyform_admin";
		} catch (Exception e) {
			bindingResult.reject("modifyFailed", "수정 중 오류가 발생했습니다: " + e.getMessage());
			model.addAttribute("uno", uno);
			userForm.setUserId(userToModify.getUserId());
			return "user_mypage_modifyform_admin";
		}
		return "redirect:/user/list"; // 관리자 목록 페이지 경로
	}

	// 회원삭제
	@GetMapping("/delete/{uno}")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public String deleteUser(@PathVariable("uno") Long uno) {
		users.deleteUser(uno);
		return "redirect:/user/list";
	}

	// "내 정보 수정" 폼 (GET)
	@PreAuthorize("isAuthenticated()") // 로그인한 사용자만 접근 가능
	@GetMapping("/modify")
	public String myModifyForm(Model model, Principal principal, UserModifyForm userModifyForm) {

		// 현재 로그인한 사용자의 ID로 User 객체를 조회합니다.
		User user = users.getUser(principal.getName());

		userModifyForm.setEmail(user.getEmail()); // 폼에 현재 이메일 설정
		model.addAttribute("user", user); // 이름 표시용

		// 현재 사용자 역할에 다라 다른 activeMenu 값을 전달
		if (user.getRole() == UserRole.ROLE_INSTRUCTOR) {
			model.addAttribute("activeMenu", "modify"); // 강사 레이아웃(instructor_mypage_layout)용
		} else {
			// (수강생 레이아웃(user_mypage_layout)용 - 예시: "info")
			model.addAttribute("activeMenu", "info");
		}

		return "user_mypage_modifyform_self";
	}

	// [추가] "내 정보 수정" 폼을 제출(처리)하기 위한 메서드 (POST)
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/modify")
	public String myModifyUser(@Valid UserModifyForm userModifyForm, BindingResult bindingResult, Principal principal,
			Model model, RedirectAttributes redirectAttributes) {

		// 1. 현재 로그인한 사용자의 정보를 가져옵니다.
		User currentUser = users.getUser(principal.getName());
		model.addAttribute("user", currentUser); // 폼 다시 보여줄 때 ID/이름 표시용

		String activeMenu = (currentUser.getRole() == UserRole.ROLE_INSTRUCTOR) ? "modify" : "info";
		model.addAttribute("activeMenu", activeMenu);

		// 2. DTO 유효성 검사
		if (bindingResult.hasErrors()) {
			// 폼을 다시 보여주기 위해 "userForm" 객체를 그대로 사용
			return "user_mypage_modifyform_self";
		}

		// 3. 현재 비밀번호 필수
		if (userModifyForm.getCurrentPassword() == null || userModifyForm.getCurrentPassword().isBlank()) {
		bindingResult.rejectValue("currentPassword", "password.required", "현재 비밀번호를 입력하세요.");
		return "user_mypage_modifyform_self";
		}


		// 4. 현재 비밀번호 검증
		if (!passwordEncoder.matches(userModifyForm.getCurrentPassword(), currentUser.getPassword())) {
		bindingResult.rejectValue("currentPassword", "password.incorrect", "현재 비밀번호가 일치하지 않습니다.");
		return "user_mypage_modifyform_self";
		}


		// 5. 새 비밀번호 변경 여부 판단
		String newPw1 = userModifyForm.getNewPassword1();
		String newPw2 = userModifyForm.getNewPassword2();


		boolean newPw1Filled = newPw1 != null && !newPw1.isBlank();
		boolean newPw2Filled = newPw2 != null && !newPw2.isBlank();


		if (newPw1Filled || newPw2Filled) {
		if (!newPw1Filled || !newPw2Filled) {
		bindingResult.reject("password.partial", "새 비밀번호와 확인을 모두 입력하세요.");
		return "user_mypage_modifyform_self";
		}
		if (!newPw1.equals(newPw2)) {
		bindingResult.rejectValue("newPassword2", "password.inCorrect", "새 비밀번호 확인이 일치하지 않습니다.");
		return "user_mypage_modifyform_self";
		}
		
		// 비밀번호 패턴 검사 추가
	    String pwPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,16}$";
	    if (!newPw1.matches(pwPattern)) {
	        bindingResult.rejectValue("newPassword1", "password.invalid", "비밀번호는 8~16자, 대/소문자, 숫자, 특수문자를 포함해야 합니다.");
	        return "user_mypage_modifyform_self";
	    }


		// 비밀번호 변경 실행
		currentUser.setPassword(passwordEncoder.encode(newPw1));
		}

		try {
			// 5. UserService의 새 'modifyMyInfo' 메서드 호출
			users.modifyMyInfo(currentUser,
				    (newPw1Filled && newPw2Filled) ? newPw1 : null,// 변경할 때만 비번 전달
				    userModifyForm.getEmail()); 

			model.addAttribute("message", "회원정보가 수정되었습니다.");

		} catch (DataIntegrityViolationException e) {
			// 6. 서비스에서 이메일 중복 시 발생하는 예외 처리
			bindingResult.rejectValue("email", "email.duplicate", "이미 사용 중인 이메일입니다.");
			return "user_mypage_modifyform_self";
		} catch (Exception e) {
			bindingResult.reject("modifyFailed", "수정 중 오류가 발생했습니다: " + e.getMessage());
			return "user_mypage_modifyform_self";
		}

		// 7. 수정 완료 후, 역할에 따른 마이페이지로 리다이렉트
		if (currentUser.getRole() == UserRole.ROLE_INSTRUCTOR) {
			return "user_mypage_modifyform_self"; // 강사 마이페이지 메인
		} else {
			return "user_mypage_modifyform_self"; // 수강생 마이페이지 (기존 코드)
		}
	}

	// 마이페이지 (진행률 표시)
	@GetMapping("/mypage")
	public String myPage(Model model) {
		User user = users.getCurrentUser();
		if (user == null)
			return "redirect:/user/login";

		Long uno = user.getUno();

		// 진행률 데이터 불러오기
		long completed = enrollments.getCompletedCount(uno);
		long total = enrollments.getTotalCount(uno);
		double progress = enrollments.getProgressPercent(uno);

		model.addAttribute("user", user);
		model.addAttribute("completed", completed);
		model.addAttribute("total", total);
		model.addAttribute("progress", progress);

		return "user_mypage"; // user_mypage.html로 이동
	}

	// [추가] 관리자 승인 기능 (승인 대기 강사 목록 + 승인 처리)
	// 관리자 : 승인 대기 중인 강사 목록
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@GetMapping("/instructors/pending")
	public String getPendingInstructors(Model model) {
		List<User> pendingList = users.getAllUsers().stream()
				.filter(u -> u.getRole() == UserRole.ROLE_INSTRUCTOR && !u.isApproved()).toList();

		model.addAttribute("instructors", pendingList);
		return "user_pending_instructors"; // templates/user_pending_instructors.html
	}

	// 관리자 : 강사 승인 처리
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@PostMapping("/approve/{uno}")
	public String approveInstructor(@PathVariable("uno") Long uno) {
		User user = users.getUser(uno);
		if (user.getRole() == UserRole.ROLE_INSTRUCTOR) {
			user.setApproved(true);
			users.save(user); // UserService에 save 메서드 추가 필요
		}
		return "redirect:/user/list/unapproved"; // 승인 후 다시 비승인 강사 리스트로 복귀
	}

	// [신규] 회원 본인 탈퇴 처리 (GET)
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/delete/self")
	public String deleteSelf(Principal principal, RedirectAttributes redirectAttributes, HttpServletRequest request,
			HttpServletResponse response) {

		try {
			User currentUser = users.getUser(principal.getName());
			Long uno = currentUser.getUno();

			// 1. Spring Security에서 강제 로그아웃 처리
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth != null) {
				new SecurityContextLogoutHandler().logout(request, response, auth);
			}

			// 2. 사용자 계정 삭제
			users.deleteUser(uno);

			// 3. 로그아웃 후 메시지 전달
			redirectAttributes.addFlashAttribute("msg", "회원 탈퇴가 정상적으로 처리되었습니다. 이용해주셔서 감사합니다.");

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMsg", "회원 탈퇴 중 오류가 발생했습니다: " + e.getMessage());
			return "redirect:/mainpage"; // 오류 발생 시 메인으로
		}

		return "redirect:/mainpage"; // 성공 시 메인으로
	}
}