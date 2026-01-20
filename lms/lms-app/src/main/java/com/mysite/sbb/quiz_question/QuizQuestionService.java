package com.mysite.sbb.quiz_question;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.mysite.sbb.DataNotFoundException;
import com.mysite.sbb.quiz.Quiz;
import com.mysite.sbb.user.User;
import com.mysite.sbb.user.UserRole;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class QuizQuestionService {

	private final QuizQuestionRepository qQuestionr;
	
	// 문제 생성
    public void createQuestion(Quiz quiz, QuizQuestionForm form, User author) {
        QuizQuestion q = new QuizQuestion();
        q.setQuiz(quiz); // 부모 Quiz 설정
        q.setAuthor(author); // [중요] 문제 출제자 설정
        q.setQuizContent(form.getQuizContent());
        q.setOption1(form.getOption1());
        q.setOption2(form.getOption2());
        q.setOption3(form.getOption3());
        q.setOption4(form.getOption4());
        q.setCorrectAnswer(form.getCorrectAnswer());
        q.setScore(form.getScore());
        qQuestionr.save(q);
    }

    // 특정 퀴즈의 문제 목록 조회
    public List<QuizQuestion> getQuestionsByQuiz(Long quizId) {
        return qQuestionr.findByQuiz_QuizId(quizId);
    }
    
    // 페이징을 지원하는 서비스 메소드 + 검색어 지원
    public Page<QuizQuestion> getQuestionsByQuiz(Long quizId, int page, String kw) {
		Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "questionId"));
		
		if (kw == null || kw.trim().isEmpty()) {
			// 검색어가 없으면 기존 방식대로 조회
			return qQuestionr.findByQuizQuizId(quizId, pageable);
		} else {
			// 검색어가 있으면 검색 쿼리 실행
			return qQuestionr.findAllByKeyword(quizId, kw, pageable);
		}
	}
    
    // 단일 문제 조회
    public QuizQuestion getQuestion(Long questionId) {
        return qQuestionr.findById(questionId)
                .orElseThrow(() -> new DataNotFoundException("해당 문제를 찾을 수 없습니다. ID: " + questionId));
    }
    
    // 권한 검사 (내부 로직)
    private void checkPermission(QuizQuestion question, User currentUser) {
    	// 1. 관리자(Admin)는 항상 통과
        if (currentUser.getRole() == UserRole.ROLE_ADMIN) {
            return;
        }

        // 2. 관리자가 아닌 경우, (author가 null이거나, 내가 author가 아니면) 차단
        if (question.getAuthor() == null || !question.getAuthor().getUno().equals(currentUser.getUno())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 문제에 대한 관리 권한이 없습니다.");
        }
    }

    // 문제 수정
    public void updateQuestion(Long questionId, QuizQuestionForm form, User currentUser) {
        QuizQuestion q = getQuestion(questionId);
        
        // 문제 수정 권한 검사
        checkPermission(q, currentUser);
        
        // 폼 데이터로 엔티티 업데이트
        q.setQuizContent(form.getQuizContent());
        q.setOption1(form.getOption1());
        q.setOption2(form.getOption2());
        q.setOption3(form.getOption3());
        q.setOption4(form.getOption4());
        q.setCorrectAnswer(form.getCorrectAnswer());
        q.setScore(form.getScore());
        // 출제자(author)는 수정 시 변경하지 않음
        qQuestionr.save(q);
    }
    
    public void deleteQuestion(Long questionId, User currentUser) {
        QuizQuestion q = getQuestion(questionId);
        
        // [중요] 문제 삭제 권한 검사
        checkPermission(q, currentUser);
        
        qQuestionr.delete(q);
    }
}
