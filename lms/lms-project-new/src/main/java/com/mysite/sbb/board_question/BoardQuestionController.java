package com.mysite.sbb.board_question;

import java.security.Principal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.mysite.sbb.board_answer.BoardAnswer;
import com.mysite.sbb.board_answer.BoardAnswerForm;
import com.mysite.sbb.board_answer.BoardAnswerService;
import com.mysite.sbb.classes.Classes;
import com.mysite.sbb.classes.ClassesService;
import com.mysite.sbb.enrollment.Enrollment;
import com.mysite.sbb.enrollment.EnrollmentService;
import com.mysite.sbb.user.User;
import com.mysite.sbb.user.UserRole;
import com.mysite.sbb.user.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequestMapping("/question")
@RequiredArgsConstructor
@Controller
public class BoardQuestionController {

	// 필요한 서비스들 주입받기
	private final BoardQuestionService bqService;
	private final UserService uService;
	private final EnrollmentService eService;
	private final ClassesService cService;
	private final BoardAnswerService baService; // (10/24)
		
	// 질문 목록 (페이징)
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/list")
	public String list(Model model,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "kw", defaultValue = "") String kw,
            @RequestParam(value = "searchType", defaultValue = "title") String searchType) {
		
		// 서비스 호출 (page, searchType, kw 전달)
		Page<BoardQuestion> paging = bqService.getList(page, searchType, kw);
		
		model.addAttribute("paging", paging);
		model.addAttribute("kw", kw);
		model.addAttribute("searchType", searchType);
		
		return "board_list";
	}

	// 질문 상세
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/detail/{boardQuesId}")
	public String detail(Model model, @PathVariable("boardQuesId") Long boardQuesId, BoardAnswerForm boardAnswerForm) {
		BoardQuestion question = bqService.getQuestion(boardQuesId.longValue());

		List<BoardAnswer> answerList = baService.getAnswersForQuestion(boardQuesId);

		model.addAttribute("question", question);
		model.addAttribute("answerList", answerList); 
		return "question_detail";
	}

	// 질문 등록 폼 (GET)
	@PreAuthorize("isAuthenticated()") // 추후에 관리자/강사/학생으로 구분해야함.
	@GetMapping("/create")
	public String questionCreate(BoardQuestionForm boardQuestionForm, Model model, Principal principal,
			Pageable pageable) {
		// 1. 현재 로그인한 사용자 정보 가져오기

		User currentUser = uService.getUser(principal.getName()); // UserService에 구현 필요

		// 2. 해당 사용자의 수강목록 조회하기 (enrollmentService에 findClassesByUser 구현 필요)
		Page<Enrollment> myClasses = eService.findAllClassesByUser(currentUser, pageable);

		// 3. 조회된 목록을 모델에 담아 뷰로 전달하기
		model.addAttribute("myClasses", myClasses);

		return "board_question_form";
	}

	// 질문 등록 처리 (POST)
	@PreAuthorize("isAuthenticated()") // 추후에 관리자/강사/학생으로 구분해야함.
	@PostMapping("/create")
	public String questionCreate(@Valid BoardQuestionForm boardQuestionForm, BindingResult bindingResult, Model model,
			Principal principal, Pageable pageable) {
		User currentUser = uService.getUser(principal.getName()); // User 객체 조회
		if (bindingResult.hasErrors()) {

			Page<Enrollment> myClasses = eService.findAllClassesByUser(currentUser, pageable);
			model.addAttribute("myClasses", myClasses);
			return "board_question_form";
		}

		Classes selectedClass = cService.getClassById(boardQuestionForm.getClassId()); // (ClassesService에 구현 필요)

		bqService.create(boardQuestionForm.getTitle(), boardQuestionForm.getQuesContent(), currentUser, selectedClass);

		return "redirect:/question/list";
	}

	// 질문 수정 (GET)
		@PreAuthorize("isAuthenticated()") // 추후에 관리자/강사/학생으로 구분해야함.
		@GetMapping("/modify/{boardQuesId}")
		public String questionModify(BoardQuestionForm boardQuestionForm, @PathVariable("boardQuesId") Long boardQuesId,
				Model model, Principal principal, Pageable pageable) {

			BoardQuestion question = bqService.getQuestion(boardQuesId.longValue());

			User currentUser = uService.getUser(principal.getName()); // [추가] 현재 유저 정보 조회
			
			// [수정] 본인이 아니고, 관리자도 아니면 권한 없음
	        if (!question.getUser().getUserId().equals(principal.getName()) 
	             && currentUser.getRole() != UserRole.ROLE_ADMIN) {
	            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
	        }

			boardQuestionForm.setTitle(question.getTitle());
			boardQuestionForm.setQuesContent(question.getQuesContent());
			boardQuestionForm.setClassId(question.getClasses().getClassesId()); // 기존 강의 ID 설정

			Page<Enrollment> myClasses = eService.findAllClassesByUser(currentUser, pageable);
			model.addAttribute("myClasses", myClasses);

			model.addAttribute("questionId", boardQuesId);

			return "board_question_form";
		}


	// 질문 수정 (POST)
		@PreAuthorize("isAuthenticated()")
		@PostMapping("/modify/{boardQuesId}")
		public String questionModify(@Valid BoardQuestionForm boardQuestionForm, Model model, BindingResult bindingResult,
				Principal principal, @PathVariable("boardQuesId") Long boardQuesId, Pageable pageable) {
			
			BoardQuestion question = bqService.getQuestion(boardQuesId);
			User currentUser = uService.getUser(principal.getName());
			
			if (bindingResult.hasErrors()) {
				Page<Enrollment> myClasses = eService.findAllClassesByUser(currentUser, pageable);
				model.addAttribute("myClasses", myClasses);
				model.addAttribute("questionId", boardQuesId);

				return "board_question_form";
			}
			
			if (!question.getUser().getUserId().equals(principal.getName()) 
		             && currentUser.getRole() != UserRole.ROLE_ADMIN) {
		            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
			}

			bqService.modify(question, boardQuestionForm.getTitle(), boardQuestionForm.getQuesContent());
			return String.format("redirect:/question/detail/%s", boardQuesId);
		}

		// 질문 삭제 (GET)
		@PreAuthorize("isAuthenticated()")
		@GetMapping("/delete/{boardQuesId}")
		public String questionDelete(Principal principal, @PathVariable("boardQuesId") Long boardQuesId) {
			BoardQuestion question = bqService.getQuestion(boardQuesId);
			User currentUser = uService.getUser(principal.getName());
			
			// 권한 체크
	        if (!question.getUser().getUserId().equals(principal.getName()) 
	             && currentUser.getRole() != UserRole.ROLE_ADMIN) {
	            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
	        }
	        
			bqService.delete(question);
			return "redirect:/question/list"; // 삭제 후 메인 화면이나 목록으로 이동
		}

	// 마이페이지 내 답글
	@PreAuthorize("isAuthenticated()") // 로그인한 사용자만 접근 가능
	@GetMapping("/my")
	public String myQuestions(Model model, Principal principal,
			@RequestParam(value = "page", defaultValue = "0") int page) {

		// 1. 현재 로그인한 사용자 정보를 가져옵니다.
		User currentUser = this.uService.getUser(principal.getName());

		// 2. BoardQuestionService를 통해 현재 사용자가 작성한 질문 목록을 조회합니다.
		Pageable pageable = PageRequest.of(page, 5, Sort.by(Sort.Direction.DESC, "quesDate")); // 5개씩
		Page<BoardQuestion> paging = this.bqService.findMyQuestions(currentUser, pageable);

		// 3. Model에 질문 목록을 담아서 템플릿으로 전달
		model.addAttribute("paging", paging);
		model.addAttribute("user", currentUser);

		// 4. "내 질문" 템플릿 파일 이름을 반환
		return "user_mypage_board_question"; 
	}
}
