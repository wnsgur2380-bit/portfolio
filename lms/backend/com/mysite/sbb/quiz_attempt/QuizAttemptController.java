package com.mysite.sbb.quiz_attempt;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mysite.sbb.DataNotFoundException;
import com.mysite.sbb.quiz.Quiz;
import com.mysite.sbb.quiz.QuizService;
import com.mysite.sbb.quiz_answer.QuizAnswer;
import com.mysite.sbb.quiz_answer.QuizAnswerForm;
import com.mysite.sbb.quiz_answer.QuizAnswerListForm;
import com.mysite.sbb.quiz_answer.QuizAnswerRepository;
import com.mysite.sbb.user.User;
import com.mysite.sbb.user.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor // 자동으로 연결해주는 생성자
@Controller
@RequestMapping("/quiz_attempt/")
public class QuizAttemptController {
	
	private final QuizAttemptService qAttempts;
	private final UserService uService;
	private final QuizService qService;
	private final QuizAnswerRepository qAnswerr;
	
	// 퀴즈 응시 시작 처리 (응시 기록 생성 후, 20개의 빈 답안을 미리 만듦)
	// 파라미터 : quizId(시작할 퀴즈 ID), principal(현재 사용자), redirectAttributes(리다이렉트 메시지 전달용)
	// 리턴 : 퀴즈 풀이 페이지 또는 에러 페이지로
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/start/{quizId}")
	public String startAttempt(Model model, @PathVariable("quizId") Long quizId, Principal principal) {
		
		Quiz quiz = qService.getQuizById(quizId);
		User user = uService.getUser(principal.getName());
		
		try {
			// [수정] 서비스의 startAttempt 호출 (이 안에서 20개 문제/답안 생성)
			QuizAttempt attempt = qAttempts.startAttempt(quiz, user);
			
			// 생성된 시도(Attempt)의 응시 페이지로 리다이렉트
			return "redirect:/quiz_attempt/exam/" + attempt.getAttemptId();
			
		} catch (IllegalStateException e) {
			// 레벨 테스트 중복 응시 예외 처리
			// RedirectAttributes가 없으므로 템플릿으로 에러 전달 (혹은 classes/list로 리다이렉트)
			model.addAttribute("errorMsg", e.getMessage());
			return "redirect:/classes/list"; // 혹은 에러 페이지
		}
	}
	
	// 퀴즈 풀이 페이지 (GET) 메소드 추가 (10/28)
		@PreAuthorize("isAuthenticated()")
		@GetMapping("/exam/{attemptId}")
		public String showQuizExam(Model model, @PathVariable("attemptId") Long attemptId, Principal principal) {
			
			// 1. 응시 기록(Attempt) 조회
			QuizAttempt attempt = qAttempts.getAttemptById(attemptId);
			User currentUser = uService.getUser(principal.getName());
			
			// 2. 본인 확인
			if (!attempt.getUser().getUno().equals(currentUser.getUno())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "응시 권한이 없습니다.");
			}
			
			// 3. 이 시도(Attempt)에 연결된 20개의 'QuizAnswer' 목록을 가져옵니다.
			List<QuizAnswer> quizAnswers = qAnswerr.findByqAttempt(attempt);

			// 4. 폼을 위한 DTO(QuizAnswerListForm)를 생성합니다.
			QuizAnswerListForm answerListForm = new QuizAnswerListForm();
			List<QuizAnswerForm> formList = new ArrayList<>();
			
			// 5. 20개의 QuizAnswer 엔티티를 폼 DTO(QuizAnswerForm) 20개로 변환합니다.
			for (QuizAnswer ans : quizAnswers) {
				QuizAnswerForm form = new QuizAnswerForm();
				// 템플릿 폼(th:field)에서 사용할 ID
				form.setQAnswerId(ans.getAnswerId()); 
				form.setQQuestionId(ans.getQQuestion().getQuestionId()); 
				// 'userAnswer'는 템플릿에서 사용자가 입력합니다.
				formList.add(form);
			}
			// 20개의 폼 리스트를 래퍼(Wrapper) DTO에 설정
			answerListForm.setAnswerForms(formList); 
			
			model.addAttribute("attempt", attempt);
			model.addAttribute("quizAnswers", quizAnswers); 
			model.addAttribute("answerListForm", answerListForm);
			
			return "quiz_exam";
		}
	
		// 답안 제출 및 채점 처리 (답안 채점 후 결과 페이지로 이동)
		// 파라미터 : attemptId(응시 기록ID), answers(사용자 답안 목록), principal(현재 사용자), redirectAttributes(리다이렉트 메시지 전달용)
		// 리턴 : 결과 페이지로
		@PreAuthorize("isAuthenticated()")
		@PostMapping("/{attemptId}/submit")
		public String submitAnswers(
				@PathVariable("attemptId") Long attemptId,
				@ModelAttribute QuizAnswerListForm answerListForm,
				Authentication authentication,
				RedirectAttributes redirectAttributes,
				HttpServletRequest request // 이전 페이지 URL 얻기
				) {
			try {
				User currentUser = uService.getUser(authentication.getName());
				
				// NullPointException 방지: answerForm 또는 getAnswers()가 null일 경우 빈 리스트 사용
				List<QuizAnswerForm> submittedForms = Optional.ofNullable(answerListForm)
														.map(QuizAnswerListForm::getAnswerForms)
														.orElse(new ArrayList<>());
				
				if (submittedForms.isEmpty()) {
					throw new IllegalArgumentException("제출된 답안이 없습니다.");
				}
				
				
				// 서비스에 List<QuizAnswerForm>를 그대로 전달
				QuizAttempt resultAttempt = qAttempts.submitAnswersAndGrade(attemptId, submittedForms, currentUser);
				
				// 성공 시, 결과 페이지로 리다이렉트 (응시 ID 전달)
				return "redirect:/quiz_attempt/" + resultAttempt.getAttemptId() + "/result";
			} catch (DataNotFoundException | SecurityException | IllegalArgumentException e) {
				// 데이터 못 찾음, 권한 없음, 잘못된 답안 형식 등의 오류 처리
				redirectAttributes.addFlashAttribute("error", e.getMessage());
				// 실패 시 , 이전에 있던 퀴즈 풀이 페이지로 다시 돌려 보냄 (Referer : 이전에 있던 페이지 주소를 저장하는 기능)
				String referrer = request.getHeader("Referer"); // 이전 페이지 주소 가져오기
				// Referer 주소가 없거나 한 경우, 강의 목록 페이지로 이동
				return "redirect:" + (referrer != null ? referrer : "/classes/list");
			}
		}
	
		
		// 특정 응시 결과 조회 및 화면 표시 (채점된 응시 기록을 조회하여 결과 페이지를 보여줌)
		// 파라미터 : attemptId(조회할 응시 기록ID), principal(현재 사용자정보), model(HTML에 데이터 전달 객체)
		// 리턴 : 보여줄 뷰 이름
		@PreAuthorize("isAuthenticated()")
		@GetMapping("/{attemptId}/result")
		public String getAttemptResult(@PathVariable("attemptId") Long attemptId, Authentication authentication, Model model) {
			try {
				// 1. 현재 사용자 정보 가져오기
				User currentUser = uService.getUser(authentication.getName());
				// 2. Service 호출하여 결과 메세지가 포함된 응시 기록 객체 가져오기
				QuizAttempt attemptResult = qAttempts.getResultAttempt(attemptId, currentUser);
				// 3. Model 객체에 필요한 데이터를 담아 HTML 페이지로 전달
				String resultMessage = qAttempts.generateResultMessage(attemptResult);
				// 4. Model 객체에 응시 기록과 결과 메시지를 따로 담아 전달
				model.addAttribute("attemptResult", attemptResult);
				model.addAttribute("resultMessage", resultMessage); // 메시지 별도 전달
				
				List<QuizAnswer> answers = qAnswerr.findByqAttempt(attemptResult);
				model.addAttribute("answers", answers);
				
				// 5. 보여줄 HTML 파일의 이름 반환
				return "quiz_result";
			} catch (DataNotFoundException e) {
	            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "응시 기록을 찾을 수 없습니다.");
	        } catch (SecurityException e) {
	             throw new ResponseStatusException(HttpStatus.FORBIDDEN, "결과를 조회할 권한이 없습니다.");
	        } catch (Exception e) {
	             System.err.println("결과 조회 오류: " + e.getMessage());
	             throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "결과 조회 중 오류 발생", e);
			}
		}
		
		// 현재 로그인한 사용자의 응시기록 목록 조회 및 화면 표시
		@PreAuthorize("isAuthenticated()")
		@GetMapping("/user/attempt")
		public String getCurrentUserAttempts(Principal principal, Model model) {
			try {
				User currentUser = uService.getUser(principal.getName());
				List<QuizAttempt> attempts = qAttempts.getAttemptsByUser(currentUser.getUno());
				model.addAttribute("attemptList", attempts);
				return "my_quiz_result";
			} catch (Exception e) {
	            System.err.println("내 응시 기록 조회 오류: " + e.getMessage());
	            model.addAttribute("error", "응시 기록 로딩 중 오류 발생.");
	            return "error_page"; // 에러 페이지 템플릿 이름
	        }
		}
		
		// 특정 퀴즈의 모든 응시 기록 조회 및 화면 표시 (관리자/강사 권한)
		@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_INSTRUCTOR')")
		@GetMapping("/quiz/{quizId}")
		public String getQuizAttempts(@PathVariable("quizId") Long quizId, Model model) {
			try {
				List<QuizAttempt> attemptList = qAttempts.getAttemptsByQuiz(quizId);
				model.addAttribute("attemptList", attemptList);
				return "quiz_attempt/list";
			} catch (DataNotFoundException e) {
	             throw new ResponseStatusException(HttpStatus.NOT_FOUND, "퀴즈를 찾을 수 없습니다.");
	        } catch (Exception e) {
	            System.err.println("퀴즈별 응시 기록 조회 오류: " + e.getMessage());
	            model.addAttribute("error", "응시 기록 조회 중 오류 발생");
	            return "error_page";
		}
	}
		
	// 특정 응시 기록 삭제 처리 (관리자 권한)
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@PostMapping("/{attemptId}/delete")
	public String deleteAttempt(@PathVariable("attemptId") Long attemptId, RedirectAttributes redirectAttributes) {
		try {
			qAttempts.deleteAttempt(attemptId);
			redirectAttributes.addFlashAttribute("success", "응시 기록(ID: " + attemptId + ") 삭제 완료.");
			return "redirect:/quiz_attempt/list";
		} catch (DataNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/quiz_attempt/list";
        } catch (Exception e) {
             System.err.println("응시 기록 삭제 오류: " + e.getMessage());
             redirectAttributes.addFlashAttribute("error", "삭제 중 오류 발생.");
            return "redirect:/quiz_attempt/list";
		}
	}
}
		
