package com.mysite.sbb.enrollment;

import java.security.Principal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mysite.sbb.DataNotFoundException;
import com.mysite.sbb.user.User;
import com.mysite.sbb.user.UserService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
@RequestMapping("/enrollment/")
public class EnrollmentController {

	private final EnrollmentService enrollments;
	private final UserService uService;

	// 수강신청 (학생 전용)
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/create/{classesId}")
	public String enroll(@PathVariable("classesId") Long classesId, Principal principal,
			RedirectAttributes redirectAttributes) { // [수정] RedirectAttributes 파라미터 추가

		User currentUser = uService.getUser(principal.getName());
		// [수정] 에러/성공 메시지를 보여주기 위해 강의 상세 페이지로 리다이렉트
		String redirectUrl = "redirect:/classes/" + classesId;
		
		// ★ [추가] 권한 체크 (무료기간 만료 & 미결제 시 차단)
	    if (!uService.canAccessCourse(currentUser)) {
	        redirectAttributes.addFlashAttribute("errorMsg", "무료 체험 기간이 만료되었습니다. 결제 후 이용해주세요.");
	        return redirectUrl;
	    }

		try {
			// 1. 수강 신청 시도
			enrollments.enroll(classesId, currentUser);

			// 2. 성공 시: 성공 메시지 추가 후 리다이렉트
			redirectAttributes.addFlashAttribute("msg", "수강 신청이 완료되었습니다."); // alert.html용
			return redirectUrl; // 성공 시 강의 상세 페이지로

		} catch (IllegalStateException e) {
			// 3. [수정] "이미 수강 중" 예외(IllegalStateException)를 잡음
			// 에러 메시지를 "errorMsg" 키로 전달
			redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
			return redirectUrl; // 실패 시에도 강의 상세 페이지로

		} catch (Exception e) {
			// 4. 그 외 다른 예외 발생 시
			redirectAttributes.addFlashAttribute("errorMsg", "수강 신청 처리 중 오류가 발생했습니다.");
			return redirectUrl; // 실패 시에도 강의 상세 페이지로
		}
	}

	// [수정] 내 강의실 (학생 본인만) (페이징)
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/user")
	public String getUserEnrollments(Principal principal, Model model,
			// [수정] status 파라미터 받기 (기본값 "active")
			@RequestParam(value = "status", defaultValue = "active") String status,
			@RequestParam(value = "page", defaultValue = "0") int page) {

		User currentUser = uService.getUser(principal.getName());

		// 사용자의 전체 수강 목록을 가져옵니다.
		Pageable pageable = PageRequest.of(page, 5, Sort.by(Sort.Direction.DESC, "enrollmentDate")); // 5개씩
		Page<Enrollment> paging = enrollments.findClassesByUser(currentUser, status, pageable);

		model.addAttribute("paging", paging);
		model.addAttribute("currentStatus", status); // "active" 또는 "completed"
		model.addAttribute("user", currentUser);
		
		return "user_mypage"; // templates/user_mypage.html
	}

	// 강의별 수강생 목록 (관리자/강사용)
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/classes/{classesId}")
	public String getClassesEnrollments(@PathVariable("classesId") Long classesId, Model model,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "kw", defaultValue = "") String kw) {
		
		Page<Enrollment> paging = enrollments.getEnrollmentsByClass(classesId, page, kw);
		
		// 상단 제목 표시를 위해 강의 정보가 필요하다면 조회
        // Classes classes = cService.getClassById(classesId);
        // model.addAttribute("classes", classes);
		
		model.addAttribute("paging", paging);
		model.addAttribute("classesId", classesId);
		model.addAttribute("kw", kw);
		
		return "enrollment_classes"; // templates/enrollment_classes.html
	}

	// 진도율 업데이트 (관리자/강사만 가능)
	@PreAuthorize("isAuthenticated()")
	@PutMapping("/{enrollmentId}")
	public String updateProgress(@PathVariable("enrollmentId") Long enrollmentId, @RequestParam int progress,
			@RequestParam boolean completed) {
		enrollments.updateProgress(enrollmentId, progress, completed);
		return "redirect:/enrollment/classes"; // 수정 후 강의별 수강생 목록으로 이동
	}

	// 수강 완료 처리
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/complete/{classesId}")
	public String markAsCompleted(@PathVariable("classesId") Long classesId,
			    				// [신규] 폼에서 전송된 시청 시간(초) 받기
								@RequestParam(value = "watch_duration", defaultValue = "0") int watchDuration,
								Principal principal,
								RedirectAttributes redirectAttributes) {
				
		// [신규] 10분(600초) 시청 시간 검증
		int minDurationSeconds = 20; 
		// int minDurationSeconds = 10; // 테스트용 10초
		
		if (watchDuration < minDurationSeconds) {
			// 시간이 부족하면, errorMsg를 Flash Attribute에 담아 영상 페이지로 리다이렉트
			redirectAttributes.addFlashAttribute("errorMsg", "10분 이상 들어야 완료가 됩니다.");
			// [수정] 강의 상세가 아닌, 시청 페이지로 다시 돌아가기
			return "redirect:/classes/watch/" + classesId; 
		}

		try {
			User currentUser = uService.getUser(principal.getName());
			enrollments.markAsCompleted(currentUser, classesId);
					
			// [수정] 성공 메시지와 함께 LocalStorage 초기화 신호 전송
			redirectAttributes.addFlashAttribute("msg", "강의 수강이 완료 처리되었습니다.");
			redirectAttributes.addFlashAttribute("clearWatchTime", true); // JS가 이 값을 보고 LocalStorage를 삭제
					
		} catch (DataNotFoundException e) {
			redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
		      } catch (Exception e) {
		          redirectAttributes.addFlashAttribute("errorMsg", "처리 중 오류가 발생했습니다.");
		      }
				
		// [수정] 수강 완료 후, 영상 페이지가 아닌 강의 상세 페이지로 리다이렉트
		return "redirect:/classes/" + classesId;
	}

	// 수강 취소 (관리자/강사만 가능)
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/delete/{enrollmentId}")
	public String delete(@PathVariable("enrollmentId") Long enrollmentId, Authentication authentication) {

		Enrollment enrollment = enrollments.getEnrollmentById(enrollmentId); // 서비스에 getEnrollmentById 필요
		User currentUser = uService.getUser(authentication.getName());

		// 8. 본인 또는 관리자만 삭제할 수 있도록 권한 확인 (중요)
		if (!enrollment.getUser().getUno()
				.equals(currentUser.getUno()) /* && !currentUser.getRole().equals("ROLE_ADMIN") */) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
		}

		enrollments.delete(enrollmentId);
		return "redirect:/enrollment/user"; // 취소 후 목록으로 리다이렉트
	}
	
	// [추가] 실시간 진도율 업데이트 (AJAX용) - 20초 기준
    @PostMapping("/progress/{classesId}")
    @ResponseBody // 화면 이동 없이 데이터만 반환
    public ResponseEntity<String> updateRealtimeProgress(
            @PathVariable("classesId") Long classesId,
            @RequestParam("watched_seconds") int watchedSeconds, // 클라이언트가 보낸 시청 시간
            Principal principal) {
        
        User currentUser = uService.getUser(principal.getName());
        
        // 위에서 만든 'Fixed' 서비스 메서드 호출
        enrollments.updateRealtimeProgressFixed(currentUser, classesId, watchedSeconds);
        
        return ResponseEntity.ok("Progress Updated");
    }

}