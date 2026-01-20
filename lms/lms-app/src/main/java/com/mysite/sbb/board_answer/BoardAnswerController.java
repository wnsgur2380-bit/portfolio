package com.mysite.sbb.board_answer;

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

import com.mysite.sbb.board_question.BoardQuestion;
import com.mysite.sbb.board_question.BoardQuestionService;
import com.mysite.sbb.user.User;
import com.mysite.sbb.user.UserRole;
import com.mysite.sbb.user.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequestMapping("/answer")
@RequiredArgsConstructor
@Controller
public class BoardAnswerController {
	
	private final BoardQuestionService bqService;
    private final BoardAnswerService baService;
    private final UserService uService;
    
    // 답변 등록
    @PreAuthorize("isAuthenticated()") // 추후에 역할 구분 추가해야함
    @PostMapping("/create/{BoardQuesId}")
    public String createAnswer(Model model,
    						   @PathVariable("BoardQuesId") Long BoardQuesId,
                               @Valid BoardAnswerForm BoardanswerForm,
                               BindingResult bindingResult,
                               Principal principal) {
        
        BoardQuestion question = bqService.getQuestion(BoardQuesId.longValue());
        User currentUser = uService.getUser(principal.getName()); // Principal로 사용자 조회

        if (bindingResult.hasErrors()) {
        	List<BoardAnswer> answerList = baService.getAnswersForQuestion(BoardQuesId);
            model.addAttribute("question", question);
            model.addAttribute("answerList", answerList);
            return "question_detail";
        }

        // 서비스를 통해 답변 생성
        baService.create(question, BoardanswerForm.getAnswContent(), currentUser);

        // 답변등록 후, 상세 페이지로 이동
        return String.format("redirect:/question/detail/%s", BoardQuesId);
    }
    
 // 답변 수정 (GET)
    @PreAuthorize("isAuthenticated()") // 추후에 역할 구분 추가해야함
    @GetMapping("/modify/{boardAnswId}")
    public String answerModify(BoardAnswerForm boardAnswerForm,
    						   @PathVariable("boardAnswId") Long boardAnswId,
    						   Principal principal,
    						   Model model) {
    	
        BoardAnswer answer = baService.getAnswer(boardAnswId);
        User currentUser = uService.getUser(principal.getName());
        
     // 권한 체크 (답변 작성자 본인 or 관리자만)
        if (!answer.getUser().getUserId().equals(principal.getName()) 
             && currentUser.getRole() != UserRole.ROLE_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
        }
        boardAnswerForm.setAnswContent(answer.getAnswContent());
        
        // 템플릿에서 사용할 수 있도록 모델에 추가
        model.addAttribute("boardAnswerForm", boardAnswerForm); // 모델 속성 이름 명시
        model.addAttribute("boardAnswId", boardAnswId); // 수정 대상 ID 전달
        model.addAttribute("questionId", answer.getQuestion().getBoardQuesId()); // 질문 ID도 전달 (취소 등 이동 시 필요)
        return "answer_form"; // 답변 수정을 위한 별도 폼
    }
    
    // 답변 수정 (POST)
    @PreAuthorize("isAuthenticated()") // 추후에 역할 구분 추가해야함
    @PostMapping("/modify/{boardAnswId}")
    public String answerModify(@Valid BoardAnswerForm boardQuestionForm,
    						   BindingResult bindingResult,
                               @PathVariable("boardAnswId") Long boardAnswId,
                               Principal principal,
                               Model model) {
    	
    	BoardAnswer answer = baService.getAnswer(boardAnswId); // Answer 객체 먼저 조회
	User currentUser = uService.getUser(principal.getName());    	

        if (bindingResult.hasErrors()) {
        	model.addAttribute("BoardAnswerForm", boardQuestionForm); // 오류 시 form 객체 유지
            model.addAttribute("boardAnswId", boardAnswId);
            model.addAttribute("questionId", answer.getQuestion().getBoardQuesId()); // "취소" 버튼용
            return "answer_form";
        }
        
        // 권한 체크 (답변 작성자 본인 or 관리자)
        if (!answer.getUser().getUserId().equals(principal.getName()) 
             && currentUser.getRole() != UserRole.ROLE_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
        }
        
        baService.modify(answer, boardQuestionForm.getAnswContent());
        return String.format("redirect:/question/detail/%s", answer.getQuestion().getBoardQuesId()); // 해당 답변의 상세 페이지 이동
    }
    
    // 답변 삭제
    @PreAuthorize("isAuthenticated()") // 추후에 역할 구분 추가해야함
    @GetMapping("/delete/{boardAnswId}")
    public String answerDelete(Principal principal,
    					       @PathVariable("boardAnswId") Long boardAnswId) {
        
    	BoardAnswer answer = baService.getAnswer(boardAnswId);
    	User currentUser = uService.getUser(principal.getName());
        
    	// 권한 체크 (답변 작성자 본인 or 관리자)
        if (!answer.getUser().getUserId().equals(principal.getName()) 
             && currentUser.getRole() != UserRole.ROLE_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }
        
        baService.delete(answer);
        return String.format("redirect:/question/detail/%s", answer.getQuestion().getBoardQuesId()); // 해당 질문의 상세 페이지 이동
    }
    
  //마이페이지답글보기
    @PreAuthorize("isAuthenticated()") // 로그인한 사용자만 접근 가능
    @GetMapping("/my") // 최종 URL은 /answer/my 가 됩니다.
    public String myAnswers(Model model, Principal principal,
    		@RequestParam(value = "page", defaultValue = "0") int page) {
        
        User currentUser = uService.getUser(principal.getName());
        
        Pageable pageable = PageRequest.of(page, 5, Sort.by(Sort.Direction.DESC, "answDate")); // 5개씩
        
        Page<BoardAnswer> paging = baService.findMyAnswers(currentUser, pageable);
        
        model.addAttribute("paging", paging);
        model.addAttribute("user", currentUser);
        
        return "user_mypage_board_answer"; // templates/mypage_answers.html
    }
}
