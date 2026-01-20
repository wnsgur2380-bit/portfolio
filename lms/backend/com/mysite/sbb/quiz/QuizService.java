package com.mysite.sbb.quiz;

import java.util.List;
import org.springframework.stereotype.Service;
import com.mysite.sbb.DataNotFoundException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class QuizService {
	
	private final QuizRepository quizr;

	// 퀴즈 등록
    public Quiz createQuiz(Quiz quiz) {
        return quizr.save(quiz);
    }

    // 전체 퀴즈 목록
    public List<Quiz> getAllQuiz() {
        return quizr.findAll();
    }

    // 퀴즈 단일 조회
    public Quiz getQuizById(Long quizId) {
        return quizr.findById(quizId)
                .orElseThrow(() -> new DataNotFoundException("해당 퀴즈를 찾을 수 없습니다. ID: " + quizId));
    }

    // 퀴즈 수정
    public Quiz updateQuiz(Long quizId, Quiz updatedQuiz) {
        Quiz quiz = getQuizById(quizId);
        quiz.setQuizTitle(updatedQuiz.getQuizTitle());
        quiz.setQuizType(updatedQuiz.getQuizType());
        quiz.setTotalScore(updatedQuiz.getTotalScore());
        quiz.setLevel(updatedQuiz.getLevel());
        return quizr.save(quiz);
    }

    // 퀴즈 삭제
    public void deleteQuiz(Long quizId) {
    	quizr.deleteById(quizId);
    }
}
