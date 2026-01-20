package com.mysite.sbb.board_question;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.mysite.sbb.classes.Classes;
import com.mysite.sbb.user.User;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardQuestionService {

	private final BoardQuestionRepository bqr;

	// 전체 질문 목록 조회
	public List<BoardQuestion> getList() {
		return bqr.findAll();
	}

	// ID 기준으로 특정 질문 조회
	public BoardQuestion getQuestion(Long id) {
		Optional<BoardQuestion> question = bqr.findById(id);
		if (question.isPresent()) {
			return question.get();
		} else {
			// 실제 프로젝트에서는 사용자 정의 예외를 사용 추천
			throw new RuntimeException("Question not found");
		}
	}

	//  전체 질문 목록 조회 (페이징) + 검색 기능 포함
	public Page<BoardQuestion> getList(int page, String searchType, String kw) {
		Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "quesDate"));
        Specification<BoardQuestion> spec = getSpec(searchType, kw);
        return bqr.findAll(spec, pageable);
	}

	// 검색 쿼리 생성 메서드 (Specification)
    private Specification<BoardQuestion> getSpec(String searchType, String kw) {
        return new Specification<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Predicate toPredicate(Root<BoardQuestion> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                query.distinct(true); // 중복 제거
                
                // 검색어가 없으면 조건 없음 (모든 데이터)
                if (kw == null || kw.trim().isEmpty()) {
                    return null;
                }
                
                // 검색어가 있으면 조건 생성
                String likeKw = "%" + kw.toLowerCase() + "%";
                
                if ("class".equals(searchType)) {
                    // 1. 강의명 검색 (Classes 테이블 조인)
                    Join<BoardQuestion, Classes> cJoin = root.join("classes", JoinType.INNER);
                    return cb.like(cb.lower(cJoin.get("title")), likeKw);
                    
                } else if ("author".equals(searchType)) {
                    // 2. 글쓴이 검색 (User 테이블 조인)
                    Join<BoardQuestion, User> uJoin = root.join("user", JoinType.INNER);
                    return cb.like(cb.lower(uJoin.get("userName")), likeKw);
                    
                } else {
                    // 3. 제목 검색 (기본값)
                    return cb.like(cb.lower(root.get("title")), likeKw);
                }
            }
        };
    }
	
	// [수정] "내 질문" 목록을 가져오는 메서드 (페이징)
	public Page<BoardQuestion> findMyQuestions(User user, Pageable pageable) {
		return bqr.findByUserOrderByQuesDateDesc(user, pageable);
	}

	public void create(String title, String quesContent, User user, Classes classes) {
		BoardQuestion q = new BoardQuestion();
		q.setTitle(title); // 질문 제목
		q.setQuesContent(quesContent); // 질문 내용
		q.setUser(user); // 질문 작성자 정보
		q.setClasses(classes); // 질문 강의 정보

		bqr.save(q);
	}

	// 질문 수정
	public void modify(BoardQuestion question, String title, String quesContent) {
		question.setTitle(title);
		question.setQuesContent(quesContent);
		bqr.save(question);
	}

	// 질문 삭제
	public void delete(BoardQuestion question) {
		bqr.delete(question);
	}

	// 강사 ID로 질문 목록 조회
	public List<BoardQuestion> getQuestionsByInstructor(Long uno) {
		return bqr.findByInstructorUno(uno);
	}
	
	// InstructorController에서 페이징을 위해 호출할 메소드
    public Page<BoardQuestion> getQuestionsByInstructor(Long uno, Pageable pageable) {
        return bqr.findByInstructorUno(uno, pageable);
    }
    
    // 특정 강사의 모든 강의에 달린 질문들을 페이징하여 조회
    public Page<BoardQuestion> getQuestionsForInstructor(User instructor, int page) {
    	// 강사의 강의 목록 가져오기 
    	List<Classes> instructorClasses = instructor.getClasses();
    	
    	// 페이징 설정 (10개씩, 최신순)
    	Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "quesDate"));
    	
    	// 레포지터리 메서드 호출
    	return bqr.findByClassesInOrderByQuesDateDesc(instructorClasses, pageable);
    }
}
