package com.mysite.sbb.enrollment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mysite.sbb.classes.Classes;
import com.mysite.sbb.level.Level;
import com.mysite.sbb.user.User;


public interface EnrollmentRepository extends JpaRepository<Enrollment, Long>{

	// [수정] 사용자 기준으로 신청 목록 찾기 (페이징)
	Page<Enrollment> findByUser(User user, Pageable pageable);
	
	// [추가] 완료/진행중 페이징
	Page<Enrollment> findByUserAndCompleted(User user, boolean completed, Pageable pageable);
	
	// 특정 회원의 신청 내역
	List<Enrollment> findByUser_Uno(Long uno); 
	
	// 특정 강의의 신청자 목록
    List<Enrollment> findByClasses(Classes classes); 
    
    // 특정 회원의 특정 강의 신청
    List<Enrollment> findByClasses_ClassesId(Long classesId); 
    
    // 특정 사용자의 특정 레벨 수강 목록 조회
    List<Enrollment> findByUserAndClasses_Level(User user, Level level);
    
    // 중복 수강신청 방지용 (회원 + 강의 조합)
    boolean existsByUser_UnoAndClasses_ClassesId(Long uno, Long classesId);
    
    // 사용자와 강의 객체로 수강 정보 조회 (수강 완료 처리를 위함)
    Optional<Enrollment> findByUserAndClasses(User user, Classes classes);
    
    // 강사 ID(uno)로 수강내역 목록 조회 (수강생 관리 위함)
    List<Enrollment> findByClasses_User_Uno(Long instructorUno);
    
    // 로그인한 사용자 기준으로 수강 완료(true)한 강의 개수
    long countByUser_UnoAndCompletedTrue(Long uno);

    // 로그인한 사용자 기준 전체 수강 강의 개수
    long countByUser_Uno(Long uno);
    
    // 특정 사용자가 특정 레벨에서 완료(true)한 강의 개수
    long countByUserAndClasses_LevelAndCompletedTrue(User user, Level level);
    
    // (상세 페이지용) 특정 강의 ID의 총 수강신청 인원 수
    long countByClasses_ClassesId(Long classesId);
    
    // (목록 페이지용) 여러 개의 강의(List<Classes>)에 대해 각각의 수강신청 인원 수
    @Query("SELECT e.classes.classesId, COUNT(e.id) " +
            "FROM Enrollment e " +
            "WHERE e.classes IN :classes " +
            "GROUP BY e.classes.classesId")
    List<Object[]> countEnrollmentsByClasses(@Param("classes") List<Classes> classes);
    
 // [추가] 특정 강의의 수강생 목록 조회 (검색 + 페이징)
    @Query("select e from Enrollment e "
            + "where e.classes.classesId = :classesId "
            + "and (lower(e.user.userName) like lower(concat('%', :kw, '%')) "
            + "or lower(e.user.userId) like lower(concat('%', :kw, '%')))")
    Page<Enrollment> findByClassesIdAndKeyword(@Param("classesId") Long classesId, @Param("kw") String kw, Pageable pageable);
}