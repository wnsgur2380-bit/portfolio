package com.mysite.sbb.quiz_question;

import java.security.Principal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mysite.sbb.quiz.Quiz;
import com.mysite.sbb.quiz.QuizService;
import com.mysite.sbb.user.User;
import com.mysite.sbb.user.UserRole;
import com.mysite.sbb.user.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/quiz_question/")
public class QuizQuestionController {
	
	private final QuizQuestionService qqService;
	private final QuizService qService;
	private final UserService uService;
	
	// 퀴즈의 문제 목록 (instructor_quiz_list.html에서 '문제 관리' 클릭 시 이동)
	@GetMapping("/list/{quizId}")
	public String list(Model model, @PathVariable("quizId") Long quizId, 
		                  QuizQuestionForm quizQuestionForm, Principal principal,
		                  @RequestParam(value = "page", defaultValue = "0") int page,
		                  @RequestParam(value = "kw", defaultValue = "") String kw) {
		
		Quiz quiz = qService.getQuizById(quizId);
		
		Page<QuizQuestion> paging = qqService.getQuestionsByQuiz(quizId, page, kw);
		
		model.addAttribute("quiz", quiz);	
		model.addAttribute("paging", paging);
		model.addAttribute("quizQuestionForm", quizQuestionForm);
		model.addAttribute("kw", kw);
			
		// (출제자 폼에 표시용)
	    if (principal != null) {
	        model.addAttribute("currentUser", uService.getUser(principal.getName()));
	    }
			
		return "instructor_quiz_question_list";
	}
	
	// 퀴즈 문제 생성
	@PreAuthorize("hasRole('ROLE_INSTRUCTOR') or hasRole('ROLE_ADMIN')")
	@PostMapping("/create/{quizId}")
	public String createQuestion(Model model, @PathVariable("quizId") Long quizId,
								 @Valid QuizQuestionForm quizQuestionForm, 
								 BindingResult bindingResult, Principal principal,
								 RedirectAttributes redirectAttributes) {
			
		Quiz quiz = qService.getQuizById(quizId);
		User currentUser = uService.getUser(principal.getName());	
		
		if (bindingResult.hasErrors()) {
			List<QuizQuestion> questionList = qqService.getQuestionsByQuiz(quizId);
			model.addAttribute("quiz", quiz);
			model.addAttribute("questionList", questionList);
			model.addAttribute("currentUser", currentUser); // 오류 시 현재 사용자 정보 다시 전달
			return "instructor_quiz_question_list";
		}
		
		try {
			// [수정] 서비스 호출을 새 시그니처로 변경 (Form DTO와 author 전달)
			qqService.createQuestion(quiz, quizQuestionForm, currentUser);
			redirectAttributes.addFlashAttribute("msg", "새 문제가 등록되었습니다.");
			
		} catch (ResponseStatusException e) {
			// [추가] 서비스단에서 권한 예외 등이 발생한 경우
			bindingResult.reject("createFailed", e.getMessage());
			List<QuizQuestion> questionList = qqService.getQuestionsByQuiz(quizId);
			model.addAttribute("quiz", quiz);
			model.addAttribute("questionList", questionList);
			model.addAttribute("currentUser", currentUser);
			return "instructor_quiz_question_list";
		}
			
		return String.format("redirect:/quiz_question/list/%s", quizId);
	}


    // 퀴즈 문제 수정 (GET)
 	@PreAuthorize("hasRole('ROLE_INSTRUCTOR') or hasRole('ROLE_ADMIN')")
 	@GetMapping("/modify/{id}")
 	public String modifyQuestion(QuizQuestionForm quizQuestionForm, @PathVariable("id") Long id, 
 	                             Model model, Principal principal) { 
 		
 		QuizQuestion qq = qqService.getQuestion(id);
 		
 		// 권한 확인
 		User currentUser = uService.getUser(principal.getName());
 		
         if (currentUser.getRole() != UserRole.ROLE_ADMIN && (qq.getAuthor() == null || !qq.getAuthor().getUno().equals(currentUser.getUno()))) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
         }
         
 		quizQuestionForm.setQuizContent(qq.getQuizContent());
 		quizQuestionForm.setOption1(qq.getOption1());
 		quizQuestionForm.setOption2(qq.getOption2());
 		quizQuestionForm.setOption3(qq.getOption3());
 		quizQuestionForm.setOption4(qq.getOption4());
 		quizQuestionForm.setCorrectAnswer(qq.getCorrectAnswer());
 		quizQuestionForm.setScore(qq.getScore()); // 점수
 		quizQuestionForm.setQuizId(qq.getQuiz().getQuizId()); // 퀴즈 ID
 		
 		model.addAttribute("quizQuestionForm", quizQuestionForm);
 		model.addAttribute("questionId", id); 
 		model.addAttribute("quiz", qq.getQuiz()); 
 		model.addAttribute("currentUser", currentUser);
 		
 		return "instructor_quiz_question_form";
 	}
 	
 	// 퀴즈 문제 수정 (POST)
 	@PreAuthorize("hasRole('ROLE_INSTRUCTOR') or hasRole('ROLE_ADMIN')")
 	@PostMapping("/modify/{id}")
 	public String modifyQuestion(@Valid QuizQuestionForm quizQuestionForm, BindingResult bindingResult,
 								 @PathVariable("id") Long id, Model model, Principal principal,
 								 RedirectAttributes redirectAttributes) { 
 		
 		User currentUser = uService.getUser(principal.getName());
 		QuizQuestion qq = qqService.getQuestion(id);
 		
 		if (bindingResult.hasErrors()) {
 			model.addAttribute("questionId", id);  
 			model.addAttribute("quiz", qq.getQuiz()); 
 			model.addAttribute("currentUser", currentUser);
 			return "instructor_quiz_question_form";
 		}
 		
 		try {
	 		// 서비스 호출을 새 시그니처로 변경 (Form DTO와 currentUser 전달)
	 		// 서비스 내부에서 권한 검사 수행
	 		qqService.updateQuestion(id, quizQuestionForm, currentUser);
	 		redirectAttributes.addFlashAttribute("msg", "문제가 수정되었습니다."); // [추가]
 		
 		} catch (ResponseStatusException e) {
 			
			bindingResult.reject("updateFailed", e.getMessage());
 			model.addAttribute("questionId", id);
 			model.addAttribute("quiz", qq.getQuiz());
 			model.addAttribute("currentUser", currentUser);
 			return "instructor_quiz_question_form";
 		}
 		
 		return String.format("redirect:/quiz_question/list/%s", qq.getQuiz().getQuizId());
 	}
 	
 	// 퀴즈 문제 삭제
  	@PreAuthorize("hasRole('ROLE_INSTRUCTOR') or hasRole('ROLE_ADMIN')")
  	@GetMapping("/delete/{id}")
  	public String deleteQuestion(@PathVariable("id") Long id, Principal principal,
  								 RedirectAttributes redirectAttributes) { 
  		
  		QuizQuestion qq = qqService.getQuestion(id);
  		User currentUser = uService.getUser(principal.getName());
  		
  		try {
 	 		// 서비스 호출을 새 시그니처로 변경 (ID와 currentUser 전달)
 	 		// 서비스 내부에서 권한 검사 수행
 	 		qqService.deleteQuestion(id, currentUser);
 	 		redirectAttributes.addFlashAttribute("msg", "문제가 삭제되었습니다."); 
  		
  		} catch (ResponseStatusException e) {
  			// 권한 예외
  			redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
  		} catch (Exception e) {
  			// 기타 삭제 오류
  			redirectAttributes.addFlashAttribute("errorMsg", "문제를 삭제할 수 없습니다.");
  		}
          
  		return String.format("redirect:/quiz_question/list/%s", qq.getQuiz().getQuizId());
  	}
}
