package com.mysite.sbb.user;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mysite.sbb.board_question.BoardQuestion;
import com.mysite.sbb.board_question.BoardQuestionService;
import com.mysite.sbb.classes.Classes;
import com.mysite.sbb.classes.ClassesService;
import com.mysite.sbb.enrollment.Enrollment;
import com.mysite.sbb.enrollment.EnrollmentService;
import com.mysite.sbb.quiz.Quiz;
import com.mysite.sbb.quiz.QuizService;
import com.mysite.sbb.quiz_question.QuizQuestion;
import com.mysite.sbb.quiz_question.QuizQuestionForm;
import com.mysite.sbb.quiz_question.QuizQuestionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/instructor") // 강사 전용 URL 경로
public class InstructorController {

	private final UserService uService;
	private final BoardQuestionService bqService;
	private final EnrollmentService eService;
	private final QuizService qService;
	private final QuizQuestionService qqService;
	private final ClassesService cService;

	// 강사 마이페이지 메인
	@PreAuthorize("hasAnyRole('ROLE_INSTRUCTOR', 'ROLE_ADMIN')")
	@GetMapping("/mypage")
	public String instructorMypageRedirect() {
		// 이 컨트롤러의 /classes URL로 다시 요청을 보냄
		return "redirect:/instructor/classes";
	}

	// 강사 마이페이지 - 내 강의 관리 (페이징 + 검색 적용)
	@PreAuthorize("hasAnyRole('ROLE_INSTRUCTOR', 'ROLE_ADMIN')")
	@GetMapping("/classes")
	public String instructorClasses(Model model, Principal principal,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "levelId", required = false) Long levelId,
			@RequestParam(value = "kw", defaultValue = "") String kw) {

		// 1. 현재 로그인한 강사 정보 조회
		User currentUser = uService.getUser(principal.getName());

		Page<Classes> paging = cService.getInstructorClasses(currentUser, page, levelId, kw);

		// [추가] 각 강의별 수강생 수 계산
		Map<Long, Long> enrollmentCountMap = eService.getEnrollmentCountsForClasses(paging.getContent());

		// 4. 템플릿으로 데이터 전달
		model.addAttribute("instructor", currentUser);
		model.addAttribute("paging", paging); // [수정] classesList 대신 paging 객체 전달
		model.addAttribute("enrollmentCountMap", enrollmentCountMap);
		model.addAttribute("activeMenu", "classes"); // 사이드바 활성화
		model.addAttribute("levelId", levelId);
		model.addAttribute("kw", kw);
		model.addAttribute("activeMenu", "classes"); // ✅ 사이드바 활성화용
        model.addAttribute("currentURI", "/instructor/classes"); // ✅ 사이드바 표시 조건용

		// 5. 템플릿 렌더링
		return "instructor_mypage_classes";
	}

	// 강사 마이페이지 - 내 강의 질문 목록
	@PreAuthorize("hasRole('ROLE_INSTRUCTOR')")
	@GetMapping("/mypage/questions")
	public String instructorMyQuestions(Model model, Principal principal,
			@RequestParam(value = "page", defaultValue = "0") int page) {

		User currentUser = uService.getUser(principal.getName());

		// 서비스 호출
		Page<BoardQuestion> paging = bqService.getQuestionsForInstructor(currentUser, page);

		model.addAttribute("paging", paging);
		model.addAttribute("pageTitle", "내 강의 질문"); // 페이지 제목
		model.addAttribute("currentURI", "/instructor/mypage/questions");
		model.addAttribute("activeMenu", "questions");

		// 템플릿 렌더링
		return "instructor_mypage_questions";
	}

	// 강사 전용 '내 수강생 목록' 페이지(
	@PreAuthorize("hasRole('ROLE_INSTRUCTOR')")
	@GetMapping("/students")
	public String instructorStudentList(Model model, Principal principal) {
		User currentUser = uService.getUser(principal.getName());

		// 1. 강사의 모든 강의에 대한 수강 내역을 가져옵니다.
		List<Enrollment> enrollments = eService.getEnrollmentsForInstructor(currentUser.getUno());

		// 2. 수강 내역에서 중복된 학생을 제거. (한 학생이 여러 강의를 수강해도 목록에는 한번만)
		Set<User> students = enrollments.stream().map(Enrollment::getUser).collect(Collectors.toSet());

		model.addAttribute("students", students); // 학생 목록(Set<User>)
		model.addAttribute("enrollments", enrollments); // 전체 수강 내역(List<Enrollment>)

		return "instructor_student_list";
	}

	// 학생별 수강 상세 조회 메소드
	@PreAuthorize("hasAnyRole('ROLE_INSTRUCTOR', 'ROLE_ADMIN')")
	@GetMapping("/student/{studentUno}")
	public String instructorStudentDetail(@PathVariable("studentUno") Long studentUno, Model model,
			Principal principal) {
		// 1. 현재 로그인한 강사 정보
		User instructor = uService.getUser(principal.getName());
		Long instructorUno = instructor.getUno();

		// 2. 조회하려는 학생 정보
		User student = uService.getUser(studentUno);

		// 3. 학생이 수강하는 모든 강의 목록
		List<Enrollment> allStudentEnrollments = eService.getByUser(studentUno);

		// 4. 학생의 수강 목록 중 내 강의만 필터링
		List<Enrollment> relevantEnrollments = allStudentEnrollments.stream()
				.filter(enrollment -> enrollment.getClasses() != null && enrollment.getClasses().getUser() != null
						&& enrollment.getClasses().getUser().getUno().equals(instructorUno))
				.collect(Collectors.toList());

		model.addAttribute("student", student);
		model.addAttribute("enrollments", relevantEnrollments); // 필터링된 목록 전달

		return "instructor_student_detail"; // 강사가 조회하는 수강학생 상세페이지
	}

	// 1. 테스트 관리 목록 페이지 (GET)
	@PreAuthorize("hasAnyRole('ROLE_INSTRUCTOR', 'ROLE_ADMIN')")
	@GetMapping("/quiz/list")
	public String instructorQuizList(Model model) {
		model.addAttribute("activeMenu", "quiz");
		model.addAttribute("currentURI", "/instructor/quiz/list");

		// 레벨 테스트와 승급 테스트만 필터링하여 조회
		List<Quiz> quizzes = qService.getAllQuiz().stream()
				.filter(q -> q.getQuizType() != null
						&& (q.getQuizType().equals("LEVEL_TEST") || q.getQuizType().startsWith("PROMOTION_TEST")))
				.collect(Collectors.toList());
		model.addAttribute("quizzes", quizzes);

		return "instructor_quiz_list";
	}

	// 2. 퀴즈 문제 목록 페이지 (GET)
	@PreAuthorize("hasAnyRole('ROLE_INSTRUCTOR', 'ROLE_ADMIN')")
	@GetMapping("/quiz/{quizId}/questions")
	public String instructorQuizQuestionList(@PathVariable("quizId") Long quizId, Model model, Principal principal,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "kw", defaultValue = "") String kw) {

		Quiz quiz = qService.getQuizById(quizId);
		User currentUser = uService.getUser(principal.getName());

		Page<QuizQuestion> paging = qqService.getQuestionsByQuiz(quizId, page, kw);

		model.addAttribute("quiz", quiz);
		model.addAttribute("paging", paging);
		model.addAttribute("kw", kw);
		model.addAttribute("currentUser", currentUser);
		model.addAttribute("currentURI", "/instructor/quiz/list");
		return "instructor_quiz_question_list";
	}

	// ====== 테스트 문제(QuizQuestion) CRUD =======

	// 퀴즈 문제 생성 폼 (GET)
	@PreAuthorize("hasAnyRole('ROLE_INSTRUCTOR', 'ROLE_ADMIN')")
	@GetMapping("/quiz/{quizId}/question/create")
	public String createQuizQuestionForm(@PathVariable("quizId") Long quizId, QuizQuestionForm quizQuestionForm,
			Model model) {

		Quiz quiz = qService.getQuizById(quizId);
		quizQuestionForm.setQuizId(quizId); // 폼에 quiz id 설정
		quizQuestionForm.setScore(5); // 문제 기본 배점 (5점)

		model.addAttribute("quiz", quiz); // 템플릿 상단에 퀴즈 이름 표시용

		return "instructor_quiz_question_form";
	}

	// 퀴즈 문제 *생성* 처리 (POST) - [신규]
	@PreAuthorize("hasAnyRole('ROLE_INSTRUCTOR', 'ROLE_ADMIN')")
	@PostMapping("/quiz/{quizId}/question/create")
	public String createQuizQuestion(@PathVariable("quizId") Long quizId, @Valid QuizQuestionForm quizQuestionForm,
			BindingResult bindingResult, Model model, Principal principal, RedirectAttributes redirectAttributes) {

		Quiz quiz = qService.getQuizById(quizId);
		User currentUser = uService.getUser(principal.getName()); // 현재 사용자가 author

		if (bindingResult.hasErrors()) {
			model.addAttribute("quiz", quiz);
			return "instructor_quiz_question_form"; // 오류 시, 생성 폼으로
		}

		// 서비스 호출 시 author(currentUser) 전달
		qqService.createQuestion(quiz, quizQuestionForm, currentUser);

		redirectAttributes.addFlashAttribute("msg", "새 문제가 등록되었습니다.");
		return "redirect:/instructor/quiz/" + quizId + "/questions";
	}

	// 퀴즈 문제 *수정* 폼 (GET) - (기존 코드 수정)
	@PreAuthorize("hasAnyRole('ROLE_INSTRUCTOR', 'ROLE_ADMIN')")
	@GetMapping("/question/edit/{questionId}")
	public String instructorQuizQuestionEditForm(@PathVariable("questionId") Long questionId,
			QuizQuestionForm quizQuestionForm, Model model, Principal principal) {

		QuizQuestion q = qqService.getQuestion(questionId);
		User currentUser = uService.getUser(principal.getName()); // [추가]

		// 관리자(Admin)는 통과
		if (currentUser.getRole() != UserRole.ROLE_ADMIN && // 관리자가 아니고
				(q.getAuthor() == null || !q.getAuthor().getUno().equals(currentUser.getUno())) // author가 null이거나 내 것이
																								// 아니면
		) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 문제에 대한 수정 권한이 없습니다.");
		}

		// 폼 객체에 기존 데이터 채우기 (기존 코드)
		quizQuestionForm.setQuizId(q.getQuiz().getQuizId());
		quizQuestionForm.setQuizContent(q.getQuizContent());
		quizQuestionForm.setOption1(q.getOption1());
		quizQuestionForm.setOption2(q.getOption2());
		quizQuestionForm.setOption3(q.getOption3());
		quizQuestionForm.setOption4(q.getOption4());
		quizQuestionForm.setCorrectAnswer(q.getCorrectAnswer());
		quizQuestionForm.setScore(q.getScore());

		model.addAttribute("questionId", questionId); // form action 경로에 사용
		model.addAttribute("quiz", q.getQuiz()); // [추가] 템플릿 상단 표시용
		model.addAttribute("quizQuestionForm", quizQuestionForm);

		return "instructor_quiz_question_form";
	}

	// 퀴즈 문제 *수정* 처리 (POST) - (기존 코드 수정)
	@PreAuthorize("hasAnyRole('ROLE_INSTRUCTOR', 'ROLE_ADMIN')")
	@PostMapping("/question/edit/{questionId}")
	public String instructorQuizQuestionEdit(@PathVariable("questionId") Long questionId,
			@Valid QuizQuestionForm quizQuestionForm, BindingResult bindingResult, Model model, Principal principal,
			RedirectAttributes redirectAttributes) {

		User currentUser = uService.getUser(principal.getName());
		QuizQuestion q = qqService.getQuestion(questionId);

		if (bindingResult.hasErrors()) {
			model.addAttribute("questionId", questionId);
			model.addAttribute("quiz", q.getQuiz());
			return "instructor_quiz_question_form";
		}

		try {
			// 서비스의 updateQuestion 호출 (폼 DTO와 currentUser 전달)
			// 서비스 내부에서 권한 검사 수행
			qqService.updateQuestion(questionId, quizQuestionForm, currentUser);

		} catch (ResponseStatusException e) {

			bindingResult.reject("updateFailed", e.getMessage());
			model.addAttribute("questionId", questionId);
			model.addAttribute("quiz", q.getQuiz());
			return "instructor_quiz_question_form";
		}

		redirectAttributes.addFlashAttribute("msg", "문제가 수정되었습니다.");
		// q.getQuiz().getQuizId()를 사용하여 문제 목록으로 복귀
		return "redirect:/instructor/quiz/" + q.getQuiz().getQuizId() + "/questions";
	}

	// 퀴즈 문제 *삭제* (GET)
	@PreAuthorize("hasAnyRole('ROLE_INSTRUCTOR', 'ROLE_ADMIN')")
	@GetMapping("/question/delete/{questionId}")
	public String deleteQuizQuestion(@PathVariable("questionId") Long questionId, Principal principal,
			RedirectAttributes redirectAttributes) {

		User currentUser = uService.getUser(principal.getName());
		QuizQuestion q = qqService.getQuestion(questionId); // 퀴즈 ID를 알아내기 위해 먼저 조회
		Long quizId = q.getQuiz().getQuizId();

		try {
			// 서비스 호출 시 currentUser 전달
			qqService.deleteQuestion(questionId, currentUser); // 서비스에서 권한 검사 및 삭제
			redirectAttributes.addFlashAttribute("msg", "문제가 삭제되었습니다.");
		} catch (ResponseStatusException e) {
			redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMsg", "문제를 삭제할 수 없습니다.");
		}

		return "redirect:/instructor/quiz/" + quizId + "/questions";
	}

}
