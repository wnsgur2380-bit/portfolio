package com.mysite.sbb;

import java.io.IOException;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.mysite.sbb.quiz.Quiz;
import com.mysite.sbb.quiz.QuizRepository;
import com.mysite.sbb.quiz_attempt.QuizAttempt;
import com.mysite.sbb.quiz_attempt.QuizAttemptService;
import com.mysite.sbb.user.User;
import com.mysite.sbb.user.UserRole;
import com.mysite.sbb.user.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserService userService;
    private final QuizRepository quizRepository;
    private final QuizAttemptService quizAttemptService;

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
		http
			.authorizeHttpRequests((authorHttpRequests) -> authorHttpRequests
					.requestMatchers(new  AntPathRequestMatcher("/**")).permitAll()
					)
			.csrf((csrf)-> csrf
					.ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**")))
			.headers((headers)-> headers
					.addHeaderWriter(new XFrameOptionsHeaderWriter(
							XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)))
			.formLogin((formLogin)-> formLogin
					.loginPage("/user/login") 
					.usernameParameter("userid") 
                    .successHandler(customAuthenticationSuccessHandler())
			        .failureHandler((request, response, exception) -> {
			            String errorMessage;
			            if (exception.getMessage().contains("관리자 승인")) {
			                errorMessage = "관리자 승인 시 로그인 및 이용 가능합니다.";
			            }else {
			                errorMessage = "아이디 또는 비밀번호를 확인하세요.";
			            }
			            request.getSession().setAttribute("errorMsg", errorMessage);
			            response.sendRedirect("/user/login?error=true");
			        })
			)
			.logout((logout)->logout
				.logoutRequestMatcher(new AntPathRequestMatcher("/user/logout"))
				.logoutSuccessUrl("/")
				.invalidateHttpSession(true));
		return http.build();
	}	
	
    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {
                
                String userId = authentication.getName();
                User user = userService.getUser(userId);

                // 1. 강사 또는 관리자는 메인페이지로 이동
                if (user.getRole() == UserRole.ROLE_INSTRUCTOR || user.getRole() == UserRole.ROLE_ADMIN) {
                    response.sendRedirect("/mainpage");
                    return;
                }

                // 2. 수강생(ROLE_LEARNER)인 경우 레벨 테스트 확인
                if (user.getRole() == UserRole.ROLE_LEARNER) {
                    try {
                        // 2-1. "LEVEL_TEST" 퀴즈 정보 찾기
                        Optional<Quiz> levelTestQuizOpt = quizRepository.findByQuizType("LEVEL_TEST");
                        
                        if (levelTestQuizOpt.isPresent()) {
                            Quiz levelTestQuiz = levelTestQuizOpt.get();
                            
                            // 2-2. 사용자의 가장 최근 "LEVEL_TEST" 응시 기록 찾기
                            Optional<QuizAttempt> attemptOpt = quizAttemptService.findLatestAttemptByUserAndQuiz(user, levelTestQuiz);

                            if (attemptOpt.isPresent()) {
                                QuizAttempt attempt = attemptOpt.get();
                                // 2-3. 점수가 0점이면 (아직 안 풀었으면) 퀴즈 페이지로 강제 이동
                                if (attempt.getScore() == 0) { 
                                    response.sendRedirect("/quiz_attempt/exam/" + attempt.getAttemptId());
                                    return;
                                }
                            }
                        }
                        // 2-4. 퀴즈가 없거나, 응시 기록이 없거나, 이미 푼(점수가 0이 아닌) 경우
                        response.sendRedirect("/mainpage");

                    } catch (Exception e) {
                        // 예외 발생 시 안전하게 메인페이지로
                        response.sendRedirect("/mainpage");
                    }
                } else {
                    // 기타 경우 메인페이지로
                    response.sendRedirect("/mainpage");
                }
            }
        };
    }
	@Bean
	AuthenticationManager am (AuthenticationConfiguration ac) throws Exception{
		return ac.getAuthenticationManager();
	}
}