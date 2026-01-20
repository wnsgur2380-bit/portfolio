package com.mysite.sbb.quiz_attempt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mysite.sbb.DataNotFoundException;
import com.mysite.sbb.level.Level;
import com.mysite.sbb.level.LevelService;
import com.mysite.sbb.quiz.Quiz;
import com.mysite.sbb.quiz_answer.QuizAnswer;
import com.mysite.sbb.quiz_answer.QuizAnswerForm;
import com.mysite.sbb.quiz_answer.QuizAnswerRepository;
import com.mysite.sbb.quiz_question.QuizQuestion;
import com.mysite.sbb.quiz_question.QuizQuestionRepository;
import com.mysite.sbb.user.User;
import com.mysite.sbb.user.UserService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class QuizAttemptService {
	
	private final QuizAttemptRepository qAttemptr;
	private final QuizAnswerRepository qAnswerr;
	private final QuizQuestionRepository qQuestionr;
	
	@Lazy
	private final UserService uService; // User 정보 접근 및 수정 위해
	private final LevelService lService; // Level 정보 접근 위해
	
	
	// 응시 생성 (퀴즈 시작)
	@Transactional
    public QuizAttempt startAttempt(Quiz quiz, User user) {
		
		List<QuizQuestion> randomQuestions = qQuestionr.findRandom20QuestionsByQuizId(quiz.getQuizId());
		
		if (randomQuestions.isEmpty()) {
			throw new RuntimeException("이 퀴즈에 등록된 문제가 없습니다. Quiz ID: " + quiz.getQuizId());
		}
		
		// 레벨 테스트 중복 응시 방지
		if ("LEVEL_TEST".equalsIgnoreCase(quiz.getQuizType())) {
			boolean alreadyAttempted = qAttemptr.existsByUserAndQuiz_QuizType(user, "LEVEL_TEST");
			if (alreadyAttempted) {
				throw new IllegalStateException("이미 레벨 테스트에 응시하셨습니다!");
			}
		}
		
		// 퀴즈 응시 생성
		QuizAttempt attempt = new QuizAttempt(); // 새 응시 기록 생성
		attempt.setQuiz(quiz); // 어떤 퀴즈인지
		attempt.setUser(user); // 누가 응시하는지
		attempt.setAttemptedCdate(LocalDateTime.now()); // 지금 시간 기록
		attempt.setScore(0); // 점수는 0점으로 시작
		
		QuizAttempt savedAttempt = qAttemptr.save(attempt);
		
		// 20개의 빈 답변 생성
		for (QuizQuestion question : randomQuestions) {
			QuizAnswer answer = new QuizAnswer();
			answer.setQAttempt(savedAttempt); // 방금 생성된 응시와 연결
			answer.setQQuestion(question); // 랜덤으로 뽑힌 문제와 연결
			answer.setCorrect(false); // 기본값 (채점 전)
			qAnswerr.save(answer);
		}
		
		return savedAttempt;
    }
	
	
	// 답안 제출 및 채점
	@Transactional // 답안 저장, 점수 업데이트, 사용자 레벨 업데이트 등 여러 DB 작업을 하므로
	// 파라미터 : 응시ID, submittedAnswers(사용자가 제출한 답안 목록), 현재사용자, 최종 점수와 결과 메세지가 포함된 객체(QuizAttempt)
	public QuizAttempt submitAnswersAndGrade(long attemptId, 
			List<QuizAnswerForm> submittedForms, User currentUser) {
		// 1. 응시 기록 찾기
		QuizAttempt attempt = getAttemptById(attemptId);
		
		// 2. 본인 확인
		if (!attempt.getUser().getUno().equals(currentUser.getUno())) {
			throw new SecurityException("자신의 응시 기록에만 답안을 제출할 수 있습니다.");
		}
		
		// 중복 제출 방지 (score가 0이 아니면 이미 채점된 것으로 간주)
		if (attempt.getScore() != 0) {
			System.out.println("이미 제출된 퀴즈입니다. ((Attempt ID: " + attemptId + ")");
			return attempt; // 이미 채점했다면, 기존 기록을 반환
		}
		
		// 3. 채점 시작
		int totalScore = 0; // 총점 계산용 변수
		Quiz quiz = attempt.getQuiz(); // 이 응시에 해당하는 퀴즈 정보 가져오기
		
		// 4. 제출된 답안 목록(List)를 하나씩 확인
		for (QuizAnswerForm submittedForm : submittedForms) {
			
			// 폼의 'qAnswerId'로 DB에 미리 생성된 QuizAnswer 레코드를 찾습니다.
						QuizAnswer savedAnswer = qAnswerr.findById(submittedForm.getQAnswerId())
			                    .orElseThrow(() -> new DataNotFoundException("답안 레코드를 찾을 수 없습니다. ID: " + submittedForm.getQAnswerId()));
						
						// 이 답안이 현재 attemptId에 속한 것이 맞는지 이중 확인
						if (!savedAnswer.getQAttempt().getAttemptId().equals(attemptId)) {
							throw new SecurityException("잘못된 답안이 제출되었습니다. (Attempt ID 불일치)");
						}
						
						// userAnswer가 null인 경우 빈 문자열로 처리 (객관식 미선택 시)
						String userAnswer = Optional.ofNullable(submittedForm.getUserAnswer()).orElse("");
						
						// 사용자가 선택한 답안을 DB 레코드에 '업데이트'
						savedAnswer.setUserAnswer(userAnswer); 
						
						// 답안에 해당하는 문제 정보 가져오기 (이미 연결되어 있음)
						QuizQuestion question = savedAnswer.getQQuestion();
						
						// 정답 비교
						boolean isCorrect = question.getCorrectAnswer().equalsIgnoreCase(userAnswer);
						savedAnswer.setCorrect(isCorrect); // 채점 결과(맞음/틀림) '업데이트'
						
						// qAttempt와 qQuestion은 startAttempt에서 이미 설정했으므로 다시 설정할 필요 없음
						
						qAnswerr.save(savedAnswer); // ★★★ 변경된 답안(userAnswer, isCorrect)을 DB에 '업데이트'
						
						// 맞았으면 점수 합산
						if (isCorrect) {
							totalScore += question.getScore(); // 문제에 설정된 점수 더하기
						}
					}
		// ---- 채점 끝 ----
		
		// 5. 계산된 총점을 응시 기록(Attempt)에 업데이트
		attempt.setScore(totalScore);
			
		// --- 결과 처리 (레벨 테스트 / 승급 테스트로 구분) ---
		String resultMessage = ""; // 결과 메시지 저장용 변수
		if ("LEVEL_TEST".equalsIgnoreCase(quiz.getQuizType())) {
			// 레벨 테스트 결과 처리: 점수에 따라 사용자 레벨 설정
			Level assignedLevel;
			// 점수 기준 (80점 이상 고급, 60점 이상 중급, 그 외 초급)
			if (attempt.getScore() >= 80) assignedLevel = lService.getLevel(3L); // 고급
			else if (attempt.getScore() >= 60) assignedLevel = lService.getLevel(2L); // 중급
            else assignedLevel = lService.getLevel(1L); // 초급
			
			User userToUpdate = uService.getUser(currentUser.getUno()); // ID로 DB에서 사용자 다시 조회
            userToUpdate.setLevel(assignedLevel); // 조회한 객체의 레벨을 변경
            uService.save(userToUpdate); // 변경된 객체를 저장
			
			resultMessage = "레벨 테스트 결과에 따라 " + assignedLevel.getLevelName() + "단계 학습부터 시작합니다!";
			
			// 승급 테스트 타입 확인
		} else if (quiz.getQuizType() != null && quiz.getQuizType().startsWith("PROMOTION_TEST")) {
			// 승급 테스트 결과 처리
			int passingScore = 80; // 합격 기준 점수 80점
			if (attempt.getScore() >= passingScore) {
				User userToUpdate = uService.getUser(currentUser.getUno()); // ID로 DB에서 사용자 다시 조회
				Level currentLevel = quiz.getLevel(); // 현재 레벨
				Level nextLevel = null; // 다음 레벨
				
				// 다음 레벨 결정 (levelId 기준)
				if (currentLevel.getLevelId() == 1L) nextLevel = lService.getLevel(2L); // 초급 -> 중급
				else if (currentLevel.getLevelId() == 2L) nextLevel = lService.getLevel(3L); // 중급 -> 고급
					// 승급 대상인지 확인 코드 추가 (사용자 레벨 정보 DB업데이트)
				if (nextLevel != null) {
					if (userToUpdate.getLevel().getLevelId() < nextLevel.getLevelId()) {
						userToUpdate.setLevel(nextLevel);
						uService.save(userToUpdate);
						resultMessage = "축하합니다! 이제 " + nextLevel.getLevelName() + "단계 강의를 수강하실 수 있습니다.";
					} else {
                        resultMessage = "테스트를 통과했지만, 이미 " + nextLevel.getLevelName() + "단계를 통과하셨습니다.";
                    }
				} else {
					resultMessage = "이미 모든 난이도의 테스트를 통과하셨습니다!";
				}
           } else {
               resultMessage = "아쉽지만 합격 기준 점수를 넘지 못하셨습니다. 차근차근 다시 복습해보고 도전해보세요!";
           }
	    }
	// --- 결과 처리 끝 ---
		
	// 6. 응시 기록(Attempt) DB에 최종 저장
	QuizAttempt savedAttempt = qAttemptr.save(attempt);
	
	// 7. 결과 메시지를 임시로 담아서 반환
	System.out.println("퀴즈 결과 메시지: " + resultMessage); // 임시로 콘솔 출력
		
	return savedAttempt; // 최종 점수가 업데이트된 응시 기록 반환	
}
	
	// ----- CRUD 메서드들 -----
	
	
	
    // 특정 회원(uno)의 모든 응시 기록 조회
    public List<QuizAttempt> getAttemptsByUser(Long uno) {
        return qAttemptr.findByUserUno(uno);
    }

    // 특정 퀴즈(quizId)의 모든 응시 기록 조회
    public List<QuizAttempt> getAttemptsByQuiz(Long quizId) {
        return qAttemptr.findByQuizQuizId(quizId);
    }
    
    // 특정 응시 기록(attemptId) 조회
    public QuizAttempt getAttemptById(Long attemptId) {
        return qAttemptr.findById(attemptId)
                .orElseThrow(() -> new DataNotFoundException("응시 기록을 찾을 수 없습니다. ID: " + attemptId));
    }

    // 특정 응시 기록(attemptId) 및 관련 답안 삭제
    @Transactional
    public void deleteAttempt(Long attemptId) {
    	QuizAttempt attempt = getAttemptById(attemptId); // [수정] 먼저 조회
    	
    	// 이 시도(Attempt)에 연결된 모든 QuizAnswer를 먼저 삭제
    	List<QuizAnswer> answers = qAnswerr.findByqAttempt(attempt);
    	qAnswerr.deleteAll(answers);
    	
    	// 그 다음 QuizAttempt 삭제
        qAttemptr.delete(attempt);
    }
    
    // 결과를 다시 볼 때 쓰일 메서드
    public QuizAttempt getResultAttempt(Long attemptId, User currentUser) {
    	// 1. ID로 응시 기록 찾기 (없으면 Exception 발생)
    	QuizAttempt attempt = getAttemptById(attemptId);
    	
    	// 2. 본인 확인 (본인이 아니면 SecurityException 발생)
    	if (!attempt.getUser().getUno().equals(currentUser.getUno())) {
    		throw new SecurityException("자신의 퀴즈 결과만 조회할 수 있습니다.");
    	}
    	// 3. 찾은 응시 기록 반환
    	return attempt;
    }
    
    // 최근 퀴즈 응시 내역 조회 메서드
    public Optional<QuizAttempt> findLatestAttemptByUserAndQuiz(User user, Quiz quiz) {
    	
        return qAttemptr.findFirstByUserAndQuizOrderByAttemptedCdateDesc(user, quiz);
    }
    
    
   // 결과 메세지를 생성하는 메서드
   public String generateResultMessage(QuizAttempt attempt) {
    	// 1. 결과 메시지를 담을 변수 준비
    	String resultMessage = "";
    	Quiz quiz = attempt.getQuiz(); // 해당 응시 기록의 퀴즈 정보 가져오기
    	
    	// 2. 퀴즈 종류에 따라 결과 메시지 생성
    	if ("LEVEL_TEST".equalsIgnoreCase(quiz.getQuizType())) {
            // 레벨 테스트 결과 메시지 생성
            Level assignedLevel;
            if (attempt.getScore() >= 80) assignedLevel = lService.getLevel(3L); // 고급
            else if (attempt.getScore() >= 60) assignedLevel = lService.getLevel(2L); // 중급
            else assignedLevel = lService.getLevel(1L); // 초급
            resultMessage = "레벨 테스트 결과에 따라 " + assignedLevel.getLevelName() + "단계 학습부터 시작합니다!";
    } else if ("PROMOTION_TEST".equalsIgnoreCase(quiz.getQuizType())) {
        // 승급 테스트 결과 메시지 생성
        int passingScore = 80; // 합격 기준 점수
        if (attempt.getScore() >= passingScore) {
             Level currentLevel = quiz.getLevel();
             Level nextLevel = null;
             if (currentLevel.getLevelId() == 1L) nextLevel = lService.getLevel(2L); // 초급 -> 중급
             else if (currentLevel.getLevelId() == 2L) nextLevel = lService.getLevel(3L); // 중급 -> 고급

             if (nextLevel != null) {
                resultMessage = "축하합니다! 이제 " + nextLevel.getLevelName() + "단계 강의를 수강하실 수 있습니다.";
             } else {
                 resultMessage = "이미 모든 난이도의 테스트를 통과하셨습니다!";
             }
        } else {
            resultMessage = "아쉽지만 합격 기준 점수를 넘지 못하셨습니다. 차근차근 다시 복습해보고 도전해보세요!";
        }
    }
    	return resultMessage;
    }
}