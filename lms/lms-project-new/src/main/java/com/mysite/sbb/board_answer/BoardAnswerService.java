package com.mysite.sbb.board_answer;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.mysite.sbb.DataNotFoundException;
import com.mysite.sbb.board_question.BoardQuestion;
import com.mysite.sbb.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardAnswerService {

	private final BoardAnswerRepository baRepository;

	// ID로 답변을 조회하는 메소드
	public BoardAnswer getAnswer(Long id) {
		return baRepository.findById(id).orElseThrow(() -> new DataNotFoundException("답변을 찾을 수 없습니다!"));
	}

	// 특정 질문에 달린 모든 답변 목록을 조회 (10/24)
	public List<BoardAnswer> getAnswersForQuestion(Long questionId) {
		return baRepository.findByQuestionBoardQuesId(questionId);
	}

	// 특정 질문에 대한 답변 등록
	public void create(BoardQuestion question, String answContent, User user) {
		BoardAnswer answer = new BoardAnswer();
		answer.setAnswContent(answContent);
		answer.setQuestion(question);
		answer.setUser(user);
		baRepository.save(answer);
	}

	// 답변 수정
	public void modify(BoardAnswer answer, String answContent) {
		answer.setAnswContent(answContent);
		baRepository.save(answer);
	}

	// 답변 삭제
	public void delete(BoardAnswer answer) {
		baRepository.delete(answer);
	}

	// "내 답글" 목록 조회
	public Page<BoardAnswer> findMyAnswers(User user, Pageable pageable) {
		return baRepository.findByUserOrderByAnswDateDesc(user, pageable);
	}
}
