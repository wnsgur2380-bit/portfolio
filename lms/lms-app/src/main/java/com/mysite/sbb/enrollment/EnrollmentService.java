package com.mysite.sbb.enrollment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mysite.sbb.DataNotFoundException;
import com.mysite.sbb.classes.Classes;
import com.mysite.sbb.classes.ClassesRepository;
import com.mysite.sbb.level.Level;
import com.mysite.sbb.user.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class EnrollmentService {

	private final EnrollmentRepository enrollmentr;
	private final ClassesRepository classesr;

	// 로그인한 회원의 수강신청
	public void enroll(Long classesId, User currentUser) {

		// 강의 유효성 검증
		Classes classes = classesr.findById(classesId)
				.orElseThrow(() -> new IllegalArgumentException("잘못된 접근입니다. (강의 정보가 유효하지 않습니다.)"));

		// 회원 유효성 검증 (로그인 세션에서 받은 currentUser)
		if (currentUser == null || currentUser.getUno() == null) {
			throw new IllegalArgumentException("로그인 정보가 유효하지 않습니다.");
		}

		// 중복 수강신청 방지
		boolean exists = enrollmentr.existsByUser_UnoAndClasses_ClassesId(currentUser.getUno(), classesId);
		if (exists) {
			throw new IllegalStateException("이미 수강 중인 강의입니다.");
		}

		// 수강신청 저장
		Enrollment enrollment = new Enrollment();
		enrollment.setClasses(classes);
		enrollment.setUser(currentUser);
		enrollmentr.save(enrollment);
	}

	// 특정 회원의 수강 목록 (내 강의실)
	public List<Enrollment> getByUser(Long uno) {
		return enrollmentr.findByUser_Uno(uno);
	}

	// [수정] 현재 로그인 사용자의 수강 목록 (페이징)
	public Page<Enrollment> findClassesByUser(User currentUser, String status, Pageable pageable) {
		if ("completed".equals(status)) {
			// "완료강좌" (completed == true)
			return enrollmentr.findByUserAndCompleted(currentUser, true, pageable);
		} else {
			// "신청강좌" (completed == false) - 기본값
			return enrollmentr.findByUserAndCompleted(currentUser, false, pageable);
		}
	}

	// [추가] 현재 로그인 사용자의 모든 수강 목록 (페이징, 질문 등록/수정 시 사용)
	public Page<Enrollment> findAllClassesByUser(User currentUser, Pageable pageable) {
		return enrollmentr.findByUser(currentUser, pageable); // Repository의 findByUser 호출
	}

	// enrollment ID로 수강 정보 조회 (권한 확인용)
	public Enrollment getEnrollmentById(Long enrollmentId) {
		return enrollmentr.findById(enrollmentId)
				.orElseThrow(() -> new DataNotFoundException("수강 정보를 찾을 수 없습니다. ID: " + enrollmentId));
	}

	// 특정 강의의 수강생 목록
	public List<Enrollment> getByClasses(Long classesId) {
		return enrollmentr.findByClasses_ClassesId(classesId);
	}
	
	// [추가] 강의별 수강생 목록 조회 (페이징 + 검색)
    public Page<Enrollment> getEnrollmentsByClass(Long classesId, int page, String kw) {
        // 10명씩 조회 (정렬: 최신 신청순 등 필요 시 Sort 추가 가능)
        Pageable pageable = PageRequest.of(page, 10); 
        
        if (kw == null) {
            kw = "";
        }
        return enrollmentr.findByClassesIdAndKeyword(classesId, kw, pageable);
    }

	// 특정 사용자의 특정 레벨 강의 진도율 계산 (80% 이상 수강 완료 여부)
	// 파라미터 : user(사용자), level(대상 레벨)
	// 리턴 : 해당 레벨 강의의 80% 이상 수강 완료 시 true, 아니면 false
	public boolean CompletedEnoughForPromotion(User user, Level level) {
		// 해당 레벨의 전체 강의 수 조회
		long totalClassesInLevel = classesr.countByLevel(level); // ClassesRepository에 countByLevel 메서드 필요

		if (totalClassesInLevel == 0) {
			return false; // 해당 레벨에 강의가 없으면 승급 불가
		}

		// 사용자가 해당 레벨에서 수강 완료(completed=true)한 강의 수 조회
		List<Enrollment> enrollments = enrollmentr.findByUserAndClasses_Level(user, level); // EnrollmentRepository에 메서드
																							// 추가 필요
		long completedClasses = enrollments.stream().filter(Enrollment::isCompleted).count();
		// :: -> 메서드 참조라는 표현식 Enrollment 안에 있는 isCompleted 메서드를 참조한다는 의미
		// isCompleted 메서드를 호출해서, 그 결과가 true인 객체들만 다음 단계로 넘긴다.
		// stream() 메서드는 컬렉션을 스트림으로 변환해서 데이터 요소들을 하나씩 처리할 수 있게 한다.

		// 진도율 계산 (완료 강의 수 / 전체 강의 수)
		double completionRate = (double) completedClasses / totalClassesInLevel;

		// 80% 이상인지 확인
		return completionRate >= 0.8;
	}

	// 진도율 업데이트 (관리자/강사용)
	@Transactional
	public void updateProgress(Long enrollmentId, int progress, boolean completed) {
		Enrollment enrollment = enrollmentr.findById(enrollmentId)
				.orElseThrow(() -> new DataNotFoundException("해당 수강 내역을 찾을 수 없습니다."));

		enrollment.setProgress(progress);
		enrollment.setCompleted(completed);
		// @Transactional 덕분에 save() 불필요 (자동 flush)
	}

	// 사용자의 수강완료 버튼 처리 메서드
	// 파라미터 : user(현재 사용자), classesId(완료할 강의 ID)
	@Transactional
	public void markAsCompleted(User user, Long classesId) {
		Classes classes = classesr.findById(classesId)
				.orElseThrow(() -> new DataNotFoundException("강의를 찾을 수 없습니다. ID: " + classesId));

		Enrollment enrollment = enrollmentr.findByUserAndClasses(user, classes)
				.orElseThrow(() -> new DataNotFoundException("수강 신청 내역이 없습니다."));

		// 이미 완료된 경우 변경하지 않음
		if (!enrollment.isCompleted()) {
			enrollment.setCompleted(true);
			enrollment.setProgress(100);
			enrollmentr.save(enrollment);
		}
	}

	// 수강 취소
	public void delete(Long enrollmentId) {
		if (!enrollmentr.existsById(enrollmentId)) {
			throw new DataNotFoundException("삭제하려는 수강 정보가 존재하지 않습니다.");
		}
		enrollmentr.deleteById(enrollmentId);
	}

	// 특정 강사의 수강생 목록(수강 내역) 조회 ---
	public List<Enrollment> getEnrollmentsForInstructor(Long instructorUno) {
		return enrollmentr.findByClasses_User_Uno(instructorUno);
	}

	// 완료한 강의수
	public long getCompletedCount(Long uno) {
		return enrollmentr.countByUser_UnoAndCompletedTrue(uno);
	}

	// 전체수강중인 강의수
	public long getTotalCount(Long uno) {
		return enrollmentr.countByUser_Uno(uno);
	}

	// 진행률
	public double getProgressPercent(Long uno) {

		long completed = getCompletedCount(uno);
		return (double) completed / 10 * 100;
	}

	// 사용자와 강의로 수강 정보 조회 (ClassesController에서 사용)
	public Optional<Enrollment> findByUserAndClasses(User user, Classes classes) {
		return enrollmentr.findByUserAndClasses(user, classes);
	}

	// 특정 레벨에서 완료한 강의 수
	public long getCompletedCountByLevel(User user, Level level) {
		if (user == null || level == null) {
			return 0;
		}
		return enrollmentr.countByUserAndClasses_LevelAndCompletedTrue(user, level);
	}
	
	// (상세 페이지용) 특정 강의의 총 수강신청 인원 수 반환
	public long getEnrollmentCount(Long classesId) {
		return enrollmentr.countByClasses_ClassesId(classesId);
	}
	
	// (목록 페이지용) 강의 목록(List)을 받아, 각 강의의 인원수를 Map<ClassesId, Count>로 반환
	public Map<Long, Long> getEnrollmentCountsForClasses(List<Classes> classes) {
		// 강의 목록이 비어있으면, 빈 Map 반환
		if (classes == null || classes.isEmpty()) {
			return new HashMap<>();
		}
		
		// Repository에서 목록을 받아옴
		List<Object[]> results = enrollmentr.countEnrollmentsByClasses(classes);
		
		// List를 Map으로 변환하여 반환
		return results.stream()
				.collect(Collectors.toMap(
						row -> (Long) row[0],
						row -> (Long) row[1]
				));
	}
	// [추가] 20초 기준 실시간 진도율 업데이트
    @Transactional
    public void updateRealtimeProgressFixed(User user, Long classesId, int watchedSeconds) {
        Classes classes = classesr.findById(classesId)
                .orElseThrow(() -> new DataNotFoundException("강의 없음"));
        
        Enrollment enrollment = enrollmentr.findByUserAndClasses(user, classes)
                .orElseThrow(() -> new DataNotFoundException("수강 내역 없음"));

        // 1. 기준 시간: 20초 (고정)
        int targetSeconds = 20; 

        // 2. 진도율 계산 ( (본 시간 / 20초) * 100 )
        int calculatedProgress = (int) ((double) watchedSeconds / targetSeconds * 100);

        // 3. 최대 100% 제한
        if (calculatedProgress > 100) calculatedProgress = 100;

        // 4. 기존 진도율보다 높을 때만 업데이트 (진도율 하락 방지)
        if (calculatedProgress > enrollment.getProgress()) {
            enrollment.setProgress(calculatedProgress);
        }

        // 5. 20초 이상(100%)이면 '수료' 처리
        if (watchedSeconds >= targetSeconds && !enrollment.isCompleted()) {
            enrollment.setCompleted(true);
            // enrollment.setEnrollmentDate(LocalDateTime.now()); // 필요 시 수료일 업데이트
        }
        
        enrollmentr.save(enrollment);
    }
}