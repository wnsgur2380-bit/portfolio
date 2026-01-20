package com.mysite.sbb;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.mysite.sbb.classes.Classes;
import com.mysite.sbb.classes.ClassesService;
import com.mysite.sbb.enrollment.EnrollmentService;
import com.mysite.sbb.level.Level;
import com.mysite.sbb.quiz.Quiz;
import com.mysite.sbb.quiz.QuizRepository;
import com.mysite.sbb.user.User;
import com.mysite.sbb.user.UserService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
public class MainController {

	private final EnrollmentService enrollments;
	private final UserService users;
	private final ClassesService classess; // 강의 리스트용 추가
	private final QuizRepository quizr;

	// 루트페이지를 메인페이지로 설정
	@GetMapping("/")
	public String root() {
		return "redirect:/mainpage";
	}

	@GetMapping("/mainpage") // "/mainpage" URL 요청 처리
	public String mainpage(Principal principal, Model model) {

		List<Classes> classesList;
		long completedCountForPromotion = 0; // 현재 레벨 완료 개수
		long totalCount = 10;
		double progressPercent = 0;
		User currentUser = null;
		boolean isGuest = false; // 로그인 안 한 상태 구분

		boolean promotionTestEligible = false;
		Long promotionTestQuizId = null;

		// 로그인한 경우
		if (principal != null) {
			currentUser = users.getUser(principal.getName());

			// 유저 레벨에 맞는 강의만 보여줌
			classesList = classess.getClassesForUser(currentUser);

			// 진행 상황 (progressPercent) 계산 기준 변경
			if (currentUser.getLevel() != null) {

				completedCountForPromotion = enrollments.getCompletedCountByLevel(currentUser, currentUser.getLevel());
			}
			progressPercent = (double) completedCountForPromotion / totalCount * 100;
			if (progressPercent > 100.0) { // 10개가 넘어도 100%
				progressPercent = 100.0;
			}

			String promotionTestType = null;
			if (currentUser.getLevel() != null) {
				Level currentLevel = currentUser.getLevel();

				if (currentLevel.getLevelId() == 1L) // 초급
					promotionTestType = "PROMOTION_TEST_BEGINNER";
				else if (currentLevel.getLevelId() == 2L) // 중급
					promotionTestType = "PROMOTION_TEST_INTERMEDIATE";
				if (promotionTestType != null) {
					promotionTestEligible = (completedCountForPromotion >= 10);

					if (promotionTestEligible) {
						Optional<Quiz> promotionQuizOpt = quizr.findByQuizType(promotionTestType);
						if (promotionQuizOpt.isPresent()) {
							promotionTestQuizId = promotionQuizOpt.get().getQuizId();
						} else {
							promotionTestEligible = false;
							System.err.println(promotionTestType + " 타입의 퀴즈를 찾을 수 없습니다.");
						}
					}
				}
			}
		}
		// 비로그인 시 전체 강의 보여줌
		else {
			classesList = classess.getAllClasses();
			isGuest = true; // 비로그인 표시
		}

		model.addAttribute("currentUser", currentUser); // 템플릿에서 ${currentUser} 사용
		model.addAttribute("classesList", classesList);
		model.addAttribute("completedCount", completedCountForPromotion);
		model.addAttribute("totalCount", totalCount);
		model.addAttribute("progressPercent", progressPercent);
		model.addAttribute("isGuest", isGuest); // 뷰에서 조건분기 가능
		model.addAttribute("promotionTestEligible", promotionTestEligible);
		model.addAttribute("promotionTestQuizId", promotionTestQuizId);

		// [추가] 랜덤 추천 강의 4개 조회
		List<Classes> randomClasses = classess.getRandomClasses(currentUser, 4);
		model.addAttribute("randomClasses", randomClasses);

		// [수정] 랜덤 추천 강사 조회 (이 코드는 기존대로 유지)
		List<User> randomInstructors = users.getRandomInstructors(4);
		model.addAttribute("randomInstructors", randomInstructors);
		
		return "mainpage";
	}

}