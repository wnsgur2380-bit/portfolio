package com.mysite.sbb.quiz_answer;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mysite.sbb.DataNotFoundException;
import com.mysite.sbb.quiz_attempt.QuizAttempt;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class QuizAnswerService {
	
	private final QuizAnswerRepository qAnswerr;
	
	// 특정 시도(attempt)의 답안 목록 조회
	public List<QuizAnswer> getAnswersByAttempt(QuizAttempt qAttempt) {
        return qAnswerr.findByqAttempt(qAttempt);
    }
	
	// 단일 답안 조회
    public QuizAnswer getAnswer(Long answerId) {
        return qAnswerr.findById(answerId)
                .orElseThrow(() -> new DataNotFoundException("해당 답안을 찾을 수 없습니다. ID: " + answerId));
    }
    
	// 답안 등록
	public QuizAnswer createAnswer(QuizAnswer quizAnswer) {
		return qAnswerr.save(quizAnswer);
	}
	
	// 답안 수정 (사용자 답변 수정 or 채점결과 변경)
    @Transactional
    public QuizAnswer updateAnswer(Long answerId, QuizAnswer updatedAnswer) {
        QuizAnswer existing = getAnswer(answerId);
        existing.setUserAnswer(updatedAnswer.getUserAnswer());
        existing.setCorrect(updatedAnswer.isCorrect());
        return qAnswerr.save(existing);
    }

    // 답안 삭제
    @Transactional
    public void deleteAnswer(Long answerId) {
        if (!qAnswerr.existsById(answerId)) {
            throw new DataNotFoundException("삭제할 답안을 찾을 수 없습니다. ID: " + answerId);
        }
        qAnswerr.deleteById(answerId);
    }

}
