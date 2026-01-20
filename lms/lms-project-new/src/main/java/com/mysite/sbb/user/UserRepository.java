package com.mysite.sbb.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	// userId로 회원 조회 (로그인 시 사용)
	Optional<User> findByUserId(String userId);

	// 이메일 중복 확인용 (회원가입 시 사용)
	Optional<User> findByEmail(String email);

	// [추가] 특정 역할을 가진 사용자를 랜덤으로 조회하는 쿼리
	@Query("SELECT u FROM User u WHERE u.role = :role ORDER BY RAND()")
	Page<User> findRandomByRole(@Param("role") UserRole role, Pageable pageable);

	// [추가] 역할별 회원 조회 (Enum 기반)
	List<User> findByRole(UserRole role);

	// [추가] 역할별 회원 조회 (Enum 기반)
	// user_list.html
	// 검색: 이름, 아이디, 이메일 포함 (role도 같이 조건에 걸 수 있도록)
	@Query("SELECT u FROM User u WHERE " + "(:roleSearch = 'ALL' OR STR(u.role) = :roleSearch) AND "
			+ "(LOWER(u.userName) LIKE %:keyword% OR LOWER(u.userId) LIKE %:keyword% OR LOWER(u.email) LIKE %:keyword%)")
	Page<User> findByKeywordAndRole(@Param("keyword") String keyword, @Param("roleSearch") String roleSearch, Pageable pageable);

	// user_list_unapproved.html
	// 비승인 강사 전체 조회
	Page<User> findByRoleAndApprovedFalse(UserRole role, Pageable pageable);

	// 비승인 강사 + 이름/아이디/이메일 검색
	@Query("SELECT u FROM User u WHERE " + "u.role = :role AND u.approved = false AND "
			+ "(LOWER(u.userName) LIKE %:keyword% OR LOWER(u.userId) LIKE %:keyword% OR LOWER(u.email) LIKE %:keyword%)")
	Page<User> searchUnapprovedInstructors(@Param("role") UserRole role, @Param("keyword") String keyword, Pageable pageable);

	// user_list_dormant.html
	// 휴면 회원 전체 조회 (1년 이상 지난 유저)
	@Query("SELECT u FROM User u WHERE u.endDate IS NOT NULL AND u.endDate <= :threshold")
	Page<User> findDormantUsers(@Param("threshold") LocalDateTime threshold, Pageable pageable);

	@Query("SELECT u FROM User u WHERE u.endDate IS NOT NULL AND u.endDate <= :threshold AND "
			+ "(LOWER(u.userName) LIKE %:keyword% OR LOWER(u.userId) LIKE %:keyword% OR LOWER(u.email) LIKE %:keyword%)")
	Page<User> findDormantUsersByKeyword(@Param("threshold") LocalDateTime threshold, @Param("keyword") String keyword, Pageable pageable);

}