package com.mysite.sbb.classes;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.mysite.sbb.DataNotFoundException;
import com.mysite.sbb.level.Level;
import com.mysite.sbb.level.LevelService;
import com.mysite.sbb.user.User;
import com.mysite.sbb.user.UserRole;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ClassesService {

	private final ClassesRepository classesr;
	private final LevelService lService;

	// 강의 등록
	public void create(ClassesForm classesForm, User user) {
		Classes classes = new Classes();
		classes.setTitle(classesForm.getTitle());
		classes.setClassesContent(classesForm.getClassesContent());
		classes.setUser(user);

		// Level 설정 추가
		Level level = lService.getLevel(classesForm.getLevelId());
		classes.setLevel(level);

		// 강의 이미지 및 비디오 추가
		classes.setClassesImg(classesForm.getClassesImg());
		classes.setClassesVideo(classesForm.getClassesVideo());

		classesr.save(classes);
	}

	// 전체 강의 목록 (리스트 페이지용)
	public List<Classes> getAllClasses() {
		return classesr.findAll();
	}

	public List<Classes> getClassesForUser(User user) {

		// 강사 또는 관리자일 경우 모든 강의 보임
		if (user != null && (user.getRole() == UserRole.ROLE_INSTRUCTOR || user.getRole() == UserRole.ROLE_ADMIN)) {
			return classesr.findAll();
		}

		// 수강자 또는 비로그인 사용자인 경우 레벨에 따라 필터링
		Long userLevelId;
		if (user != null && user.getLevel() != null) {
			userLevelId = user.getLevel().getLevelId();
		} else {
			userLevelId = 1L; // 비로그인 시 초급(1L) 강의만
		}
		return classesr.findByLevel_LevelIdLessThanEqual(userLevelId);
	}

	// 강사별 강의 목록 (마이페이지나 강사 전용 페이지용)
	public List<Classes> getClassesByUno(Long uno) {
		return classesr.findByUser_Uno(uno);
	}
	
	// 특정 강사의 강의 목록을 페이징하여 조회 (강사 마이페이지)
	public Page<Classes> getClassesByInstructor(User instructor, Pageable pageable) {
        // 1단계에서 추가한 리포지토리 메소드 호출
        return classesr.findByUser(instructor, pageable);
    }
	
	// [추가] 강사 강의 목록 조회 (수강생 순 정렬 + 검색)
    public Page<Classes> getInstructorClasses(User instructor, int page, Long levelId, String kw) {
        // 페이지 크기 6개 (정렬 조건은 쿼리에 직접 명시했으므로 PageRequest에는 페이지만 넣음)
        Pageable pageable = PageRequest.of(page, 6);
        
        // 검색어 null 처리
        if (kw == null) kw = "";
        
        // 레벨 ID 0이면 null로 처리 (전체 조회)
        Long finalLevelId = (levelId != null && levelId == 0) ? null : levelId;

        return classesr.findByInstructorWithSortAndFilter(instructor, finalLevelId, kw, pageable);
    }

	// 강의 상세보기 (상세 페이지용)
	public Classes getClassById(Long classesId) {
		return classesr.findById(classesId).orElseThrow(() -> new DataNotFoundException("해당 강의를 찾을 수 없습니다."));
	}

	// 강의 수정
	public void updateClass(Long classesId, ClassesForm classesForm, User user) {
		Classes classes = getClassById(classesId);

		if (classes.getClassesId() == null) {
			throw new IllegalArgumentException("수정하려는 강의의 ID가 존재하지 않습니다.");
		}

		classes.setTitle(classesForm.getTitle());
		classes.setClassesContent(classesForm.getClassesContent());

		Level level = lService.getLevel(classesForm.getLevelId());
		classes.setLevel(level);

		// 강의 이미지 및 비디오 추가
		classes.setClassesImg(classesForm.getClassesImg());
		classes.setClassesVideo(classesForm.getClassesVideo());

		classesr.save(classes);
	}

	// 강의 삭제
	public void deleteClass(Long classesId) {

		Classes classes = getClassById(classesId);

		// 수강생이 있는지 확인 (Classes 엔티티의 enrollments 리스트 사용)
		if (classes.getEnrollments() != null && !classes.getEnrollments().isEmpty()) {
			throw new IllegalStateException(
					"수강 중인 학생이 있어 강의를 삭제할 수 없습니다. (총 " + classes.getEnrollments().size() + "명)");
		}

		classesr.delete(classes);
	}

	// [추가]getRandomClasses 메서드 수정
	public List<Classes> getRandomClasses(User user, int limit) {
		Pageable pageable = PageRequest.of(0, limit);

		// 1. 로그인했고 레벨 정보가 있으면 -> 레벨별 랜덤 강의 조회
		if (user != null && user.getLevel() != null) {
			return classesr.findRandomClassesByLevel(user.getLevel(), pageable).getContent();
		}

		// 2. 비로그인이거나 레벨 정보가 없으면 -> 전체 랜덤 강의 조회
		return classesr.findRandomClasses(pageable).getContent();
	}

	//  페이징 및 검색 기능이 통합된 강의 목록 조회
    public Page<Classes> getList(User user, String searchType, String kw, Long levelId, Pageable pageable) {
    	Specification<Classes> spec = getSpec(searchType, kw, levelId, user);
    	return classesr.findAll(spec, pageable);
    }

    //  Specification 헬퍼 메서드 (검색 및 필터링)
    private Specification<Classes> getSpec(String searchType, String kw,Long levelId, User user) {
    	return new Specification<>() {
			private static final long SerialVersionUID = 1L;
    		
			@Override
			public Predicate toPredicate(Root<Classes> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				query.distinct(true);
				List<Predicate> predicates = new ArrayList<>();
				
				// 1. 사용자 레벨에 따른 기본 필터링 (기존 getClassesForUser 로직)
				if (user == null || user.getRole() == UserRole.ROLE_LEARNER) {
					Long userLevelId;
					if (user != null && user.getLevel() != null) {
						userLevelId = user.getLevel().getLevelId();
					} else {
						userLevelId = 3L; // 비로그인 시 모든 강의 접근은 가능(강의 보기를 막음)
					}
					predicates.add(cb.lessThanOrEqualTo(root.get("level").get("levelId"), userLevelId));
				}
				// (강사/관리자는 모든 레벨을 볼 수 있으므로 조건 없음)
				
				// 2. 검색어(kw)가 있을 경우 검색 조건 추가
				if (kw != null && !kw.trim().isEmpty()) {
					String likeKw = "%" + kw.toLowerCase() + "%"; // 검색어
					
					if ("title".equals(searchType)) {
						// 강의 제목
						predicates.add(cb.like(cb.lower(root.get("title")), likeKw));
					} else if ("instructor".equals(searchType)) {
						// 강사 이름 (User 엔티티와 조인)
						Join<Classes, User> userJoin = root.join("user", JoinType.INNER);
						predicates.add(cb.like(cb.lower(userJoin.get("userName")), likeKw));
					} else if ("instructor".equals(searchType)) {
						// 강사 이름 (Level 엔티티와 조인)
						Join<Classes, User> userJoin = root.join("user", JoinType.INNER);
						predicates.add(cb.like(cb.lower(userJoin.get("userName")), likeKw));
					}
				}
				
				// 3. 난이도(levelId) 검색 (0이 아닐 경우)
				if (levelId != null && levelId > 0) {
					predicates.add(cb.equal(root.get("level").get("levelId"), levelId));
				}
				
				return cb.and(predicates.toArray(new Predicate[0]));
			}
		};
    }
}