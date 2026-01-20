package com.mysite.sbb.board_answer;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mysite.sbb.user.User;

public interface BoardAnswerRepository extends JpaRepository<BoardAnswer, Long> {
	List<BoardAnswer> findByQuestionBoardQuesId(Long boardQuesId);
	
	//내 답글
	Page<BoardAnswer> findByUserOrderByAnswDateDesc(User user, Pageable pageable);
}
