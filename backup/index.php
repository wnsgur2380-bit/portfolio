<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Myworks - 포트폴리오</title>
    <link rel="stylesheet" href="css/style.css">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
</head>
<body>
    <!-- Navigation -->
    <nav class="navbar">
        <div class="nav-container">
            <div class="logo">
                <a href="#home">Myworks <span>*</span></a>
            </div>
            <ul class="nav-menu">
                <li><a href="#about" class="nav-link">자기소개</a></li>
                <li><a href="#portfolio" class="nav-link">포트폴리오</a></li>
                <li><a href="#career" class="nav-link">직업소개</a></li>
                <li><a href="#projects" class="nav-link">프로젝트</a></li>
                <li><a href="board.php" class="nav-link">게시판</a></li>
                <li><a href="login.php" class="nav-link contact-btn">로그인</a></li>
                <li><a href="#contact" class="nav-link contact-btn">문의하기</a></li>
            </ul>
            <div class="hamburger">
                <span></span>
                <span></span>
                <span></span>
            </div>
        </div>
    </nav>

    <!-- Hero Section -->
    <section id="home" class="hero">
        <div class="hero-content">
            <h1 class="hero-title">안녕하세요</h1>
            <p class="hero-subtitle">디자인과 개발의 완벽한 조화를 만드는 개발자입니다</p>
            <a href="#portfolio" class="cta-button">포트폴리오 보기</a>
        </div>
        <div class="hero-decoration">
            <div class="circle circle-1"></div>
            <div class="circle circle-2"></div>
            <div class="circle circle-3"></div>
        </div>
    </section>

    <!-- About Section -->
    <section id="about" class="about">
        <div class="container">
            <div class="section-header">
                <h2>자기소개</h2>
                <div class="underline"></div>
            </div>
            <div class="about-grid">
                <div class="about-content">
                    <p class="about-text">
                        저는 웹 개발과 UI/UX 디자인에 열정을 가진 개발자입니다. 
                        사용자 중심의 인터페이스와 효율적인 코드 구조를 통해 
                        최고의 사용자 경험을 제공하는 것을 목표로 합니다.
                    </p>
                    <div class="skills">
                        <h3>주요 기술</h3>
                        <div class="skill-tags">
                            <span class="skill-tag">HTML/CSS</span>
                            <span class="skill-tag">JavaScript</span>
                            <span class="skill-tag">React</span>
                            <span class="skill-tag">Node.js</span>
                            <span class="skill-tag">MongoDB</span>
                            <span class="skill-tag">UI/UX Design</span>
                        </div>
                    </div>
                </div>
                <div class="about-stats">
                    <div class="stat-item">
                        <h4>3+</h4>
                        <p>Years Experience</p>
                    </div>
                    <div class="stat-item">
                        <h4>20+</h4>
                        <p>Projects Completed</p>
                    </div>
                    <div class="stat-item">
                        <h4>100%</h4>
                        <p>Client Satisfaction</p>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- Portfolio Section -->
    <section id="portfolio" class="portfolio">
        <div class="container">
            <div class="section-header">
                <h2>포트폴리오</h2>
                <div class="underline"></div>
            </div>
            <div class="portfolio-grid">
                <div class="portfolio-item">
                    <div class="portfolio-image" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
                        <div class="portfolio-overlay">
                            <a href="#" class="portfolio-link">자세히 보기</a>
                        </div>
                    </div>
                    <div class="portfolio-info">
                        <h3>웹 채팅 애플리케이션</h3>
                        <p>Socket.IO를 활용한 실시간 다중 사용자 채팅 플랫폼</p>
                        <div class="portfolio-tags">
                            <span>Node.js</span>
                            <span>Socket.IO</span>
                            <span>Express</span>
                        </div>
                    </div>
                </div>

                <div class="portfolio-item">
                    <div class="portfolio-image" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
                        <div class="portfolio-overlay">
                            <a href="#" class="portfolio-link">자세히 보기</a>
                        </div>
                    </div>
                    <div class="portfolio-info">
                        <h3>전자상거래 플랫폼</h3>
                        <p>React와 MongoDB 기반의 풀 스택 쇼핑 플랫폼</p>
                        <div class="portfolio-tags">
                            <span>React</span>
                            <span>MongoDB</span>
                            <span>Stripe</span>
                        </div>
                    </div>
                </div>

                <div class="portfolio-item">
                    <div class="portfolio-image" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
                        <div class="portfolio-overlay">
                            <a href="#" class="portfolio-link">자세히 보기</a>
                        </div>
                    </div>
                    <div class="portfolio-info">
                        <h3>블로그 플랫폼</h3>
                        <p>마크다운 지원 및 SEO 최적화된 블로그 시스템</p>
                        <div class="portfolio-tags">
                            <span>Next.js</span>
                            <span>PostgreSQL</span>
                            <span>Tailwind</span>
                        </div>
                    </div>
                </div>

                <div class="portfolio-item">
                    <div class="portfolio-image" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);">
                        <div class="portfolio-overlay">
                            <a href="#" class="portfolio-link">자세히 보기</a>
                        </div>
                    </div>
                    <div class="portfolio-info">
                        <h3>모바일 앱</h3>
                        <p>React Native로 개발한 크로스 플랫폼 모바일 애플리케이션</p>
                        <div class="portfolio-tags">
                            <span>React Native</span>
                            <span>Firebase</span>
                            <span>Redux</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- Career Section -->
    <section id="career" class="career">
        <div class="container">
            <div class="section-header">
                <h2>직업 소개</h2>
                <div class="underline"></div>
            </div>
            <div class="career-timeline">
                <div class="timeline-item">
                    <div class="timeline-dot"></div>
                    <div class="timeline-content">
                        <h3>Senior Developer</h3>
                        <p class="company">Tech Company Inc.</p>
                        <p class="date">2023 - 현재</p>
                        <p class="description">
                            팀 리더로서 개발자 5명을 관리하며, 
                            전사 웹 플랫폼 아키텍처 설계 및 구현
                        </p>
                    </div>
                </div>

                <div class="timeline-item">
                    <div class="timeline-dot"></div>
                    <div class="timeline-content">
                        <h3>Full Stack Developer</h3>
                        <p class="company">Digital Agency</p>
                        <p class="date">2021 - 2023</p>
                        <p class="description">
                            다양한 클라이언트를 위한 웹사이트 개발 및 유지보수,
                            프론트엔드와 백엔드 모두 담당
                        </p>
                    </div>
                </div>

                <div class="timeline-item">
                    <div class="timeline-dot"></div>
                    <div class="timeline-content">
                        <h3>Junior Developer</h3>
                        <p class="company">Startup</p>
                        <p class="date">2020 - 2021</p>
                        <p class="description">
                            프론트엔드 개발 담당, 
                            React를 활용한 사용자 인터페이스 구축
                        </p>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- Projects Section -->
    <section id="projects" class="projects">
        <div class="container">
            <div class="section-header">
                <h2>개발 프로젝트</h2>
                <div class="underline"></div>
            </div>
            <div class="projects-grid">
                <div class="project-card">
                    <div class="project-icon">
                        <i class="fas fa-comments"></i>
                    </div>
                    <h3>실시간 채팅</h3>
                    <p>WebSocket 기반의 실시간 다중 사용자 채팅 시스템</p>
                </div>

                <div class="project-card">
                    <div class="project-icon">
                        <i class="fas fa-shopping-cart"></i>
                    </div>
                    <h3>전자상거래</h3>
                    <p>결제 시스템이 통합된 온라인 쇼핑 플랫폼</p>
                </div>

                <div class="project-card">
                    <div class="project-icon">
                        <i class="fas fa-book"></i>
                    </div>
                    <h3>블로그 시스템</h3>
                    <p>콘텐츠 관리 시스템이 탑재된 블로그 플랫폼</p>
                </div>

                <div class="project-card">
                    <div class="project-icon">
                        <i class="fas fa-chart-bar"></i>
                    </div>
                    <h3>데이터 대시보드</h3>
                    <p>실시간 데이터 시각화 및 분석 대시보드</p>
                </div>

                <div class="project-card">
                    <div class="project-icon">
                        <i class="fas fa-video"></i>
                    </div>
                    <h3>비디오 스트리밍</h3>
                    <p>온디맨드 비디오 스트리밍 플랫폼</p>
                </div>

                <div class="project-card">
                    <div class="project-icon">
                        <i class="fas fa-map"></i>
                    </div>
                    <h3>위치 기반 서비스</h3>
                    <p>지도 기반 위치 추적 및 공유 서비스</p>
                </div>
            </div>
        </div>
    </section>

    <!-- Contact Section -->
    <section id="contact" class="contact">
        <div class="container">
            <div class="section-header">
                <h2>연락 주세요</h2>
                <div class="underline"></div>
            </div>
            <div class="contact-content">
                <p>새로운 프로젝트에 대해 이야기하고 싶으신가요? 
                   언제든지 연락 주세요!</p>
                
                <div class="contact-info">
                    <div class="contact-item">
                        <i class="fas fa-envelope"></i>
                        <div>
                            <h4>이메일</h4>
                            <p>contact@myworks.com</p>
                        </div>
                    </div>

                    <div class="contact-item">
                        <i class="fas fa-phone"></i>
                        <div>
                            <h4>전화</h4>
                            <p>+82 10-1234-5678</p>
                        </div>
                    </div>

                    <div class="contact-item">
                        <i class="fas fa-map-marker-alt"></i>
                        <div>
                            <h4>위치</h4>
                            <p>서울, 대한민국</p>
                        </div>
                    </div>
                </div>

                <div class="social-links">
                    <a href="#" class="social-link"><i class="fab fa-github"></i></a>
                    <a href="#" class="social-link"><i class="fab fa-linkedin"></i></a>
                    <a href="#" class="social-link"><i class="fab fa-twitter"></i></a>
                    <a href="#" class="social-link"><i class="fab fa-instagram"></i></a>
                </div>
            </div>
        </div>
    </section>

    <!-- Footer -->
    <footer class="footer">
        <div class="container">
            <p>&copy; 2024 Myworks. All rights reserved.</p>
        </div>
    </footer>

    <script src="js/script.js"></script>
</body>
</html>
