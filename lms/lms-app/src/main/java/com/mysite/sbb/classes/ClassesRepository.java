package com.mysite.sbb.classes;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
//import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mysite.sbb.level.Level;
import com.mysite.sbb.user.User;

public interface ClassesRepository extends JpaRepository<Classes, Long>, JpaSpecificationExecutor<Classes>{
	
	// 강사(uno) 기준으로 강의 목록 조회
	List<Classes> findByUser_Uno(Long uno); 
	
	// 특정 레벨의 강의 개수 조회 (EnrollmentService에서 사용)
	long countByLevel(Level level);
	
	// 특정 레벨 ID 이하의 모든 강의 조회 (사용자 레벨에 맞는 강의 출력에 필요)
	List<Classes> findByLevel_LevelIdLessThanEqual(Long levelId);
	
	// [추가] 랜덤으로 강의를 조회하는 쿼리 (H2 DB는 RANDOM() 사용)
    @Query("SELECT c FROM Classes c ORDER BY RANDOM()")
    Page<Classes> findRandomClasses(Pageable pageable);
    
    // [추가] 특정 레벨(level)의 강의 중에서 랜덤 조회
    @Query("SELECT c FROM Classes c WHERE c.level = :level ORDER BY RANDOM()")
    Page<Classes> findRandomClassesByLevel(@Param("level") Level level, Pageable pageable);
    
    // [추가] User 객체와 Pageable을 받아서 해당 강사의 강의만 페이징
    Page<Classes> findByUser(User user, Pageable pageable);
    
    // [추가] 강사 마이페이지용: 검색(제목, 레벨) + 정렬(수강생 많은 순)
    @Query("SELECT c FROM Classes c " +
           "LEFT JOIN c.enrollments e " +  // 수강생 수 계산을 위해 조인
           "WHERE c.user = :instructor " + // 내 강의만 조회
           "AND (:levelId IS NULL OR c.level.levelId = :levelId) " + // 레벨 검색 (null이면 전체)
           "AND (:kw IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :kw, '%'))) " + // 제목 검색
           "GROUP BY c " + // 강의별로 그룹화
           "ORDER BY COUNT(e) DESC, c.classesCdate DESC") // 수강생 수 내림차순, 그 다음엔 최신순
    Page<Classes> findByInstructorWithSortAndFilter(
            @Param("instructor") User instructor, 
            @Param("levelId") Long levelId, 
            @Param("kw") String kw, 
            Pageable pageable);
}