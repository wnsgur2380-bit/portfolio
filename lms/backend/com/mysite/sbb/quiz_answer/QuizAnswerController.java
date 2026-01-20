package com.mysite.sbb.quiz_answer;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mysite.sbb.DataNotFoundException;
import com.mysite.sbb.quiz_attempt.QuizAttempt;
import com.mysite.sbb.quiz_attempt.QuizAttemptService;
import com.mysite.sbb.user.User;
import com.mysite.sbb.user.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
@RequestMapping("/quiz_answer/")
public class QuizAnswerController {
	
	private final QuizAttemptService qaService;
	private final UserService uService;
	private final QuizAnswerRepository qAnswerr;
	
	/**
	 * 퀴즈 제출 (채점)
	 * 설명: 20개의 답변을 QuizAnswerListForm DTO로 받아서,
	 * QuizAttemptService의 submitAnswersAndGrade 메소드로 전달합니다.
	 */
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/create/{quizAttemptId}")
	public String create(Model model, @PathVariable("quizAttemptId") Long quizAttemptId,
						 @ModelAttribute QuizAnswerListForm answerListForm, 
						 Principal principal,
						 RedirectAttributes redirectAttributes,
						 HttpServletRequest request) { 
		
		QuizAttempt attempt = qaService.getAttemptById(quizAttemptId); 
		User currentUser = uService.getUser(principal.getName());
		
		try {
			
			// [권한 확인]
			if (!attempt.getUser().getUno().equals(currentUser.getUno())) {
				throw new SecurityException("제출 권한이 없습니다.");
			}
			
			// .map() 로직 변경 -> DTO의 필드명(answerForms)으로 바로 가져옵니다.
			List<QuizAnswerForm> submittedForms = Optional.ofNullable(answerListForm)
													.map(QuizAnswerListForm::getAnswerForms) // .getAnswers() -> .getAnswerForms()
													.orElse(new ArrayList<>());
			
			if (submittedForms.isEmpty()) {
				throw new IllegalArgumentException("제출된 답안이 없습니다.");
			}

			// 서비스의 submitAnswersAndGrade 호출 (DTO 리스트 전달)
			QuizAttempt resultAttempt = qaService.submitAnswersAndGrade(
					quizAttemptId, 
					submittedForms, 
					currentUser);
			
			// 성공 시, 결과 페이지로 리다이렉트 (응시 ID 전달)
			return "redirect:/quiz_answer/quizResult/" + resultAttempt.getAttemptId();
			
		} catch (DataNotFoundException | SecurityException | IllegalArgumentException e) {
			// 데이터 못 찾음, 권한 없음, 잘못된 답안 형식 등의 오류 처리
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			// 실패 시 , 이전에 있던 퀴즈 풀이 페이지로 다시 돌려 보냄
			String referrer = request.getHeader("Referer"); 
			return "redirect:" + (referrer != null ? referrer : "/classes/list");
		}
	}
	
	/**
	 * 퀴즈 결과 페이지
	 * 설명: 채점이 완료된 20개의 QuizAnswer 엔티티를 조회하여 템플릿에 전달합니다.
	 */
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/quizResult/{quizAttemptId}")
	public String quizResult(Model model, @PathVariable("quizAttemptId") Long quizAttemptId,
							 Principal principal) {
		
		QuizAttempt attempt = qaService.getAttemptById(quizAttemptId);
		User currentUser = uService.getUser(principal.getName());
		
		// [권한 확인]
		if (!attempt.getUser().getUno().equals(currentUser.getUno())) {
			throw new SecurityException("결과 확인 권한이 없습니다.");
		}
		
		// 채점된 20개의 QuizAnswer 목록을 가져옴
		// (QuizAnswerRepository에 findByQAttempt가 구현되어 있어야 함)
		List<QuizAnswer> answers = qAnswerr.findByqAttempt(attempt);
		
		model.addAttribute("attempt", attempt);
		model.addAttribute("answers", answers); // 템플릿에 20개 답변 전달
		
		return "quiz_result";
	}
}
