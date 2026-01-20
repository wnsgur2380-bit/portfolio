package com.mysite.sbb.quiz;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.mysite.sbb.DataNotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/quiz/")
public class QuizController {

		private final QuizService qService;
		
		// 퀴즈 생성
	    @PostMapping("/create")
	    public Quiz createQuiz(@RequestBody Quiz quiz) {
	        return qService.createQuiz(quiz);
	    }

	    // 전체 퀴즈 목록
	    @GetMapping("/list")
	    public List<Quiz> getAllQuiz() {
	        return qService.getAllQuiz();
	    }

	    // 퀴즈 상세 조회
	    @GetMapping("/{quizId}")
	    public Quiz getQuiz(@PathVariable Long quizId) {
	        return qService.getQuizById(quizId);
	    }

	    // 퀴즈 수정
	    @PutMapping("/{quizId}")
	    public Quiz updateQuiz(@PathVariable Long quizId, @RequestBody Quiz quiz) {
	        return qService.updateQuiz(quizId, quiz);
	    }

	    // 퀴즈 삭제
	    @DeleteMapping("/{quizId}")
	    public ResponseEntity<String> deleteQuiz(@PathVariable Long quizId) { // 상태 코드 + 메시지
	    	qService.deleteQuiz(quizId);
	        return ResponseEntity.ok("퀴즈 삭제 완료");
	    }
	    
	    // 예외 처리 핸들러 추가
	    @ExceptionHandler(DataNotFoundException.class) // 예외 발생 시 404 JSON 반환
	    public ResponseEntity<String> handleNotFound(DataNotFoundException ex) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
	    }

}
