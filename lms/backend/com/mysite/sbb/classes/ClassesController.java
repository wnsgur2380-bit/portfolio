package com.mysite.sbb.classes;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mysite.sbb.enrollment.Enrollment;
import com.mysite.sbb.enrollment.EnrollmentService;
import com.mysite.sbb.level.Level;
import com.mysite.sbb.level.LevelService;
import com.mysite.sbb.quiz.Quiz;
import com.mysite.sbb.quiz.QuizRepository;
import com.mysite.sbb.user.User;
import com.mysite.sbb.user.UserRole;
import com.mysite.sbb.user.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
@RequestMapping("/classes/")
public class ClassesController {

	private final ClassesService cService;
	private final UserService uService;
	private final LevelService lService;
	private final EnrollmentService eService;
	private final QuizRepository quizr;

	// 강의 등록 페이지(관리자/강사용)
	@PreAuthorize("hasRole('ROLE_INSTRUCTOR') or hasRole('ROLE_ADMIN')") // 관리자 or 강사만
	@GetMapping("/create")
	public String createForm(Model model, ClassesForm classesForm) { // ClassesForm 객체 전달
		List<Level> levels = lService.getAllLevel(); // Level 목록 조회
		model.addAttribute("levels", levels); // 모델에 Level 목록 추가
		model.addAttribute("classesForm", classesForm); // 모델에 빈 폼 추가
		return "classes_create";
	}

	// 강의 등록 처리(관리자/강사용)
	@PreAuthorize("hasRole('ROLE_INSTRUCTOR') or hasRole('ROLE_ADMIN')") // 관리자 or 강사만
	@PostMapping("/create")
	public String create(@Valid @ModelAttribute("classesForm") ClassesForm classesForm, // @Valid, @ModelAttribute 사용
			BindingResult bindingResult, Principal principal, Model model) {

		if (bindingResult.hasErrors()) {
			List<Level> levels = lService.getAllLevel(); 
			model.addAttribute("levels", levels);
			return "classes_create"; 
		}

		User currentUser = uService.getUser(principal.getName()); 

		try {
			cService.create(classesForm, currentUser); 
		} catch (Exception e) {
			bindingResult.reject("createFailed", e.getMessage());
			List<Level> levels = lService.getAllLevel();
			model.addAttribute("levels", levels);
			return "classes_create";
		}

		return "redirect:/classes/list"; // 등록 후 목록으로 이동
	}

	// 전체 강의 목록 페이지 (등급업 테스트 자격 확인 로직부터는 동일)
	@GetMapping("/list")
	public String list(Model model, Principal principal, @RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "searchType", defaultValue = "title") String searchType,
			@RequestParam(value = "kw", defaultValue = "") String kw,
			@RequestParam(value = "levelId", defaultValue = "0") Long levelId) {

		User currentUser = (principal != null) ? uService.getUser(principal.getName()) : null;

		// 페이징 및 검색 로직
		Pageable pageable = PageRequest.of(page, 9, Sort.by(Sort.Direction.DESC, "classesCdate")); // 9개씩, 최신순
		Page<Classes> paging = cService.getList(currentUser, searchType, kw, levelId, pageable);

		List<Level> levels = lService.getAllLevel();

		// 수강 상태 및 수강 인원 Map 로직
		Map<Long, String> enrollmentStatusMap = new HashMap<>(); // 수강상태 Map
		Map<Long, Long> enrollmentCountMap = new HashMap<>(); // 인원수 Map

		if (currentUser != null) {
			// 현재 사용자의 모든 수강신청 내역을 가져옴
			List<Enrollment> userEnrollments = eService.getByUser(currentUser.getUno());

			enrollmentStatusMap = userEnrollments.stream()
					.collect(Collectors.toMap(enrollment -> enrollment.getClasses().getClassesId(),
							enrollment -> enrollment.isCompleted() ? "COMPLETED" : "ENROLLED",
							(status1, status2) -> status1));

		}

		// 수강 인원 수 Map 생성
		enrollmentCountMap = eService.getEnrollmentCountsForClasses(paging.getContent());

		model.addAttribute("paging", paging);
		model.addAttribute("currentUser", currentUser);
		model.addAttribute("searchType", searchType);
		model.addAttribute("kw", kw);
		model.addAttribute("levelId", levelId); // 선택된 난이도 유지
		model.addAttribute("levels", levels); // 전체 난이도 목록

		model.addAttribute("enrollmentStatusMap", enrollmentStatusMap);
		model.addAttribute("enrollmentCountMap", enrollmentCountMap);

		// 등급업 테스트 자격 확인 로직
		long completedCountForPromotion = 0; // 현재 레벨 완료 개수
		long totalCount = 10L;
		double progressPercent = 0;
		boolean promotionTestEligible = false;
		Long promotionTestQuizId = null;
		String promotionTestType = null;

		// 현재 레벨에 따라 다음 승급 테스트 타입 결정
		if (currentUser != null && currentUser.getLevel() != null) {
			Level currentLevel = currentUser.getLevel();

			if (currentLevel.getLevelId() == 1L) // 초급
				promotionTestType = "PROMOTION_TEST_BEGINNER";
			else if (currentLevel.getLevelId() == 2L) // 중급
				promotionTestType = "PROMOTION_TEST_INTERMEDIATE";

			if (promotionTestType != null) {
				// 현재 레벨에서 10개 이상 완료했는지 확인
				completedCountForPromotion = eService.getCompletedCountByLevel(currentUser, currentLevel); 
																											
				promotionTestEligible = (completedCountForPromotion >= 10);

				// 진행률 계산
				progressPercent = (double) completedCountForPromotion / totalCount * 100;
				if (progressPercent > 100.0) {
					progressPercent = 100.0;
				}

				// 진도율 충족 시 퀴즈 탐색
				if (promotionTestEligible && quizr != null) {
					Optional<Quiz> promotionQuizOpt = quizr.findByQuizType(promotionTestType);
					if (promotionQuizOpt.isPresent()) {
						promotionTestQuizId = promotionQuizOpt.get().getQuizId();
					} else {
						promotionTestEligible = false;
						System.err.println(promotionTestType + " 타입의 퀴즈를 찾을 수 없습니다.");
					}
				} else if (promotionTestEligible && quizr == null) {
					promotionTestEligible = false;
				}
			}
		}

		// --- 모델 전달 ---
		model.addAttribute("completedCount", completedCountForPromotion); // 현재 레벨 완료 카운트
		model.addAttribute("totalCount", totalCount);
		model.addAttribute("progressPercent", progressPercent);
		model.addAttribute("promotionTestEligible", promotionTestEligible);
		model.addAttribute("promotionTestQuizId", promotionTestQuizId);
		// --- 등급업 테스트 로직 끝 ---
		return "classes_list";
	}

	// 강사 마이페이지 - 내 강의 관리 (페이징 적용)
	@PreAuthorize("hasAnyRole('ROLE_INSTRUCTOR', 'ROLE_ADMIN')")
	@GetMapping("/classes")
	public String instructorClasses(Model model, Principal principal,
			@RequestParam(value = "page", defaultValue = "0") int page) {

		User currentUser = uService.getUser(principal.getName());

		Pageable pageable = PageRequest.of(page, 6, Sort.by(Sort.Direction.DESC, "classesCdate"));
		Page<Classes> paging = cService.getClassesByInstructor(currentUser, pageable);

		// [추가] 각 강의별 수강생 수 계산하여 모델에 담기
		Map<Long, Long> enrollmentCountMap = eService.getEnrollmentCountsForClasses(paging.getContent());

		model.addAttribute("instructor", currentUser);
		model.addAttribute("paging", paging);
		model.addAttribute("enrollmentCountMap", enrollmentCountMap); // [추가] 뷰로 전달
		model.addAttribute("activeMenu", "classes");

		return "instructor_mypage_classes";
	}

	// 강의 상세보기
	@GetMapping("/{classesId}")
	public String detail(@PathVariable("classesId") Long classesId, Model model, Principal principal) {
		Classes classes = cService.getClassById(classesId);
		model.addAttribute("classes", classes);

		long enrollmentCount = eService.getEnrollmentCount(classesId);

		// 수강 상태 확인 로직 추가
		boolean isEnrolled = false;
		boolean isCompleted = false;

		if (principal != null) {
			try {
				User currentUser = uService.getUser(principal.getName());
				Optional<Enrollment> enrollmentOpt = eService.findByUserAndClasses(currentUser, classes);
				if (enrollmentOpt.isPresent()) {
					isEnrolled = true;
					isCompleted = enrollmentOpt.get().isCompleted();
				}
			} catch (Exception e) {
				isEnrolled = false;
				isCompleted = false;
			}
		}
		model.addAttribute("isEnrolled", isEnrolled);
		model.addAttribute("isCompleted", isCompleted);
		model.addAttribute("enrollmentCount", enrollmentCount);

		return "classes_detail";
	}

	// 강의 수정 (강사/관리자만)
	@PreAuthorize("hasRole('ROLE_INSTRUCTOR') or hasRole('ROLE_ADMIN')")
	@GetMapping("/edit/{classesId}")
	public String editForm(@PathVariable("classesId") Long classesId, Model model, Principal principal,
			ClassesForm classesForm) {

		Classes classes = cService.getClassById(classesId);
		User currentUser = uService.getUser(principal.getName()); // 현재 사용자 정보 조회

		// 수정 권한 확인 (본인 또는 관리자)
		if (!classes.getUser().getUserId().equals(principal.getName())
				&& currentUser.getRole() != UserRole.ROLE_ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
		}

		// 기존 엔티티 데이터를 폼 객체에 설정
		classesForm.setTitle(classes.getTitle());
		classesForm.setClassesContent(classes.getClassesContent());
		classesForm.setLevelId(classes.getLevel().getLevelId());
		classesForm.setClassesImg(classes.getClassesImg());
		classesForm.setClassesVideo(classes.getClassesVideo());

		List<Level> levels = lService.getAllLevel(); // Level 목록 조회 및 전달
		model.addAttribute("levels", levels);
		model.addAttribute("classesForm", classesForm); // 데이터가 채워진 폼 전달
		model.addAttribute("classesId", classesId); // 템플릿의 form action 경로용

		return "classes_edit"; // templates/classes_edit.html
	}

	// 수정 처리 (HTML 폼)
	@PreAuthorize("hasRole('ROLE_INSTRUCTOR') or hasRole('ROLE_ADMIN')")
	@PostMapping("/edit/{classesId}")
	public String update(@PathVariable("classesId") Long classesId,
			@Valid @ModelAttribute("classesForm") ClassesForm classesForm, // @Valid, @ModelAttribute, DTO 사용
			BindingResult bindingResult, Principal principal, Model model) {

		Classes originalClasses = cService.getClassById(classesId); // 원본 데이터 로드
		User currentUser = uService.getUser(principal.getName()); // 사용자 정보 조회

		// 수정 권한 확인(본인 또는 관리자)
		if (!originalClasses.getUser().getUserId().equals(principal.getName())
				&& currentUser.getRole() != UserRole.ROLE_ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
		}

		if (bindingResult.hasErrors()) {
			List<Level> levels = lService.getAllLevel(); // 오류 시 Level 목록 다시 전달
			model.addAttribute("levels", levels);
			model.addAttribute("classesId", classesId); // form action 경로용 ID 다시 전달
			return "classes_edit"; // 유효성 검사 실패 시 폼으로
		}

		try {
			cService.updateClass(classesId, classesForm, currentUser); // 수정된 서비스 메서드 호출
		} catch (Exception e) {
			bindingResult.reject("updateFailed", e.getMessage());
			List<Level> levels = lService.getAllLevel();
			model.addAttribute("levels", levels);
			model.addAttribute("classesId", classesId);
			return "classes_edit";
		}

		return "redirect:/classes/" + classesId; // 수정 후 다시 상세 페이지로 이동
	}

	@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_INSTRUCTOR')")
	@PostMapping("/delete/{classesId}")
	public String delete(@PathVariable("classesId") Long classesId,
	                     Principal principal,
	                     RedirectAttributes redirectAttributes) {

	    Classes classes = cService.getClassById(classesId);
	    User currentUser = uService.getUser(principal.getName());

	    // 관리자이거나 해당 강의를 만든 강사일 때만 삭제 허용
	    boolean isOwner = classes.getUser().getUserId().equals(currentUser.getUserId());
	    boolean isAdmin = currentUser.getRole() == UserRole.ROLE_ADMIN;

	    if (!isOwner && !isAdmin) {
	        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
	    }

	    try {
	        cService.deleteClass(classesId);
	        redirectAttributes.addFlashAttribute("msg", "강의가 삭제되었습니다.");
	    } catch (Exception e) {
	        redirectAttributes.addFlashAttribute("errorMsg", "삭제 중 오류: " + e.getMessage());
	        return "redirect:/classes/" + classesId;
	    }

	    return "redirect:/classes/list";
	}

	// 관리자/강사용 비동기 API (선택사항)
	@PreAuthorize("hasRole('ROLE_INSTRUCTOR') or hasRole('ROLE_ADMIN')")
	@PutMapping("/{classesId}")
	@ResponseBody
	public String updateApi(@PathVariable("classesId") Long classesId, @RequestBody Classes classes) {
		classes.setClassesId(classesId);
		return "강의 수정 완료";
	}

	// 강의 삭제
	@PreAuthorize("hasRole('ROLE_INSTRUCTOR') or hasRole('ROLE_ADMIN')")
	@DeleteMapping("/{classesId}")
	@ResponseBody
	public String deleteApi(@PathVariable("classesId") Long classesId) {
		cService.deleteClass(classesId);
		return "🗑️ 강의 삭제 완료";
	}

	// [수정] 강의 영상 시청 페이지 (10분 타이머 기능용 수정)
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/watch/{classesId}")
	public String watchClassVideo(@PathVariable("classesId") Long classesId, Model model, Principal principal) {

		User currentUser = uService.getUser(principal.getName());
		
		// ★ [추가] 권한 체크
	    if (!uService.canAccessCourse(currentUser)) {
	        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이용권이 만료되었습니다. 멤버십을 구매해주세요.");
	    }
		
		Classes classes = cService.getClassById(classesId);

		// 수강 상태 확인
		Optional<Enrollment> enrollmentOpt = eService.findByUserAndClasses(currentUser, classes);
		boolean isEnrolled = enrollmentOpt.isPresent();
		boolean isCompleted = enrollmentOpt.map(Enrollment::isCompleted).orElse(false);

		// [수정] 보안 체크: 수강생이 아니고, 관리자도 아니고, 강사도 아니면 접근 불가
		// (강사와 관리자는 수강신청 없이도 영상 시청 가능)
		if (!isEnrolled && currentUser.getRole() != UserRole.ROLE_ADMIN
				&& currentUser.getRole() != UserRole.ROLE_INSTRUCTOR) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 강의를 수강하고 있지 않습니다.");
		}

		model.addAttribute("classes", classes);
		model.addAttribute("videoUrl", classes.getClassesVideo());
		model.addAttribute("isEnrolled", isEnrolled);
		model.addAttribute("isCompleted", isCompleted);
		model.addAttribute("currentUserId", currentUser.getUserId());

		return "classes_watch";
	}

	// 강사별 강의 목록 페이지 (메인페이지 추천 강사 클릭 시 이동)
    @GetMapping("/instructor/{uno}")
    public String instructorClasses(@PathVariable("uno") Long uno, Model model,
                                    @RequestParam(value = "page", defaultValue = "0") int page,
                                    @RequestParam(value = "levelId", required = false) Long levelId,
                                    @RequestParam(value = "kw", defaultValue = "") String kw) {

        // 1. 강사 정보 조회
        User instructor = uService.getUser(uno);

        // 2. 강사의 강의 목록을 '페이징'으로 조회 (검색 조건 포함)
        // (ClassesService에 이미 만들어둔 getInstructorClasses 메서드 활용)
        Page<Classes> paging = cService.getInstructorClasses(instructor, page, levelId, kw);
        
        // 3. 수강생 수 계산 (뷰에서 필요함)
        Map<Long, Long> enrollmentCountMap = eService.getEnrollmentCountsForClasses(paging.getContent());

        // 4. 모델에 데이터 전달
        model.addAttribute("instructor", instructor);
        model.addAttribute("paging", paging); // [핵심] classesList 대신 paging 전달
        model.addAttribute("enrollmentCountMap", enrollmentCountMap); // 수강생 수 맵 전달
        
        // 5. 검색 조건 유지를 위해 모델에 추가
        model.addAttribute("levelId", levelId);
        model.addAttribute("kw", kw);

        return "classes_instructor"; 
    }

}