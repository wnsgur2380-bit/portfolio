package com.mysite.sbb.board_question;



import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mysite.sbb.classes.Classes;
import com.mysite.sbb.user.User;

public interface BoardQuestionRepository extends JpaRepository<BoardQuestion, Long> {
	
	// Classes_User_Uno 메서드 수정
	@Query("SELECT bq FROM BoardQuestion bq WHERE bq.classes.user.uno = :uno")
    Page<BoardQuestion> findByInstructorUno(@Param("uno") Long uno, Pageable pageable);
	
	//  User 객체로 질문 목록을 페이징하여 조회
    Page<BoardQuestion> findByUserOrderByQuesDateDesc(User user, Pageable pageable);
    
    //  특정 사용자가 작성한 질문 (페이징)
    Page<BoardQuestion> findByUser(User user, Pageable pageable);
    //  특정 강사의 강의에 달린 질문 (List - 기존 메소드용)
    @Query("SELECT bq FROM BoardQuestion bq WHERE bq.classes.user.uno = :uno")
    List<BoardQuestion> findByInstructorUno(@Param("uno") Long uno);
    
    // 강사의 강의 목록(List<Classes>)에 포함된 모든 질문을 최신순으로 페이징하여 조회
    Page<BoardQuestion> findByClassesInOrderByQuesDateDesc(List<Classes> classesList, Pageable pageable);
    
    // Specification을 지원하는 findall 메서드
    Page<BoardQuestion> findAll(Specification<BoardQuestion> spec, Pageable pageable);
    
}
