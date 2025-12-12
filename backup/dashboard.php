<?php
session_start();

// 로그인 확인
if (!isset($_SESSION['user_id'])) {
    header('Location: login.php');
    exit;
}

// 로그아웃 처리
if (isset($_GET['logout'])) {
    session_destroy();
    header('Location: index.html');
    exit;
}
?>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>대시보드</title>
    <link rel="stylesheet" href="css/style.css">
    <style>
        .dashboard-container {
            max-width: 1200px;
            margin: 100px auto 40px;
            padding: 0 20px;
        }

        .welcome-section {
            margin-bottom: 50px;
            text-align: center;
        }

        .welcome-section h2 {
            font-size: 36px;
            font-weight: 700;
            margin-bottom: 15px;
            background: linear-gradient(135deg, #ffffff 0%, #00d4ff 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
        }

        .welcome-section p {
            color: #b0b0b0;
            font-size: 16px;
        }

        .dashboard-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 25px;
            margin-bottom: 40px;
        }

        .dashboard-card {
            background: #1a1a1a;
            border: 1px solid #333333;
            border-radius: 10px;
            padding: 30px;
            transition: all 0.3s ease;
        }

        .dashboard-card:hover {
            border-color: #00d4ff;
            box-shadow: 0 10px 30px rgba(0, 212, 255, 0.1);
            transform: translateY(-5px);
        }

        .card-icon {
            font-size: 36px;
            margin-bottom: 15px;
        }

        .card-title {
            color: #ffffff;
            font-size: 18px;
            font-weight: 700;
            margin-bottom: 10px;
            text-transform: uppercase;
            letter-spacing: 1px;
        }

        .card-content {
            color: #b0b0b0;
            font-size: 14px;
            line-height: 1.6;
        }

        .info-section {
            background: #1a1a1a;
            border: 1px solid #333333;
            border-radius: 10px;
            padding: 30px;
            margin-bottom: 30px;
        }

        .section-title {
            font-size: 20px;
            font-weight: 700;
            color: #ffffff;
            margin-bottom: 25px;
            padding-bottom: 15px;
            border-bottom: 2px solid #333333;
            text-transform: uppercase;
            letter-spacing: 1px;
        }

        .info-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
        }

        .info-item {
            background: #0d0d0d;
            padding: 20px;
            border-radius: 8px;
            border-left: 4px solid #00d4ff;
        }

        .info-label {
            color: #666666;
            font-size: 12px;
            text-transform: uppercase;
            letter-spacing: 1px;
            margin-bottom: 8px;
        }

        .info-value {
            color: #ffffff;
            font-size: 16px;
            font-weight: 600;
        }

        .button-group {
            display: flex;
            gap: 12px;
            flex-wrap: wrap;
            margin-top: 20px;
        }

        .btn {
            padding: 12px 24px;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            font-weight: 600;
            font-size: 13px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            transition: all 0.3s ease;
            text-decoration: none;
            display: inline-block;
        }

        .btn-primary {
            background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
            color: #0a0a0a;
        }

        .btn-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(0, 212, 255, 0.3);
        }

        .btn-secondary {
            background: transparent;
            color: #00d4ff;
            border: 1px solid #00d4ff;
        }

        .btn-secondary:hover {
            background: rgba(0, 212, 255, 0.1);
        }

        @media (max-width: 768px) {
            .dashboard-container {
                margin-top: 80px;
            }

            .welcome-section h2 {
                font-size: 28px;
            }

            .dashboard-grid {
                grid-template-columns: 1fr;
            }
        }
    </style>
</head>
<body>
    <nav class="navbar">
        <div class="nav-container">
            <div class="logo">
                <a href="index.html">Myworks <span>*</span></a>
            </div>
            <ul class="nav-menu">
                <li><a href="index.html#about" class="nav-link">자기소개</a></li>
                <li><a href="board.php" class="nav-link">게시판</a></li>
                <li><a href="?logout=true" class="nav-link contact-btn">로그아웃</a></li>
            </ul>
            <div class="hamburger">
                <span></span>
                <span></span>
                <span></span>
            </div>
        </div>
    </nav>

    <div class="dashboard-container">
        <section class="welcome-section">
            <h2>환영합니다, <?= htmlspecialchars($_SESSION['full_name']) ?>님</h2>
            <p>시스템 대시보드에 접근했습니다</p>
        </section>

        <div class="dashboard-grid">
            <div class="dashboard-card">
                <div class="card-icon">⚙️</div>
                <div class="card-title">시스템 상태</div>
                <div class="card-content">현재 시스템이 정상 작동 중입니다</div>
            </div>
            <div class="dashboard-card">
                <div class="card-icon">📊</div>
                <div class="card-title">통계</div>
                <div class="card-content">실시간 성능 지표를 확인하세요</div>
            </div>
            <div class="dashboard-card">
                <div class="card-icon">🔐</div>
                <div class="card-title">보안</div>
                <div class="card-content">고급 보안 설정 관리</div>
            </div>
        </div>

        <div class="info-section">
            <div class="section-title">사용자 정보</div>
            <div class="info-grid">
                <div class="info-item">
                    <div class="info-label">사용자명</div>
                    <div class="info-value"><?= htmlspecialchars($_SESSION['username']) ?></div>
                </div>
                <div class="info-item">
                    <div class="info-label">이름</div>
                    <div class="info-value"><?= htmlspecialchars($_SESSION['full_name']) ?></div>
                </div>
                <div class="info-item">
                    <div class="info-label">권한</div>
                    <div class="info-value">
                        <?php
                        $roles = [
                            'admin' => '관리자',
                            'operator' => '운영자',
                            'viewer' => '조회자'
                        ];
                        echo htmlspecialchars($roles[$_SESSION['role']] ?? '사용자');
                        ?>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-label">접근 수준</div>
                    <div class="info-value">
                        <?php
                        $access = [
                            'admin' => '전체 접근',
                            'operator' => '운영 접근',
                            'viewer' => '조회 전용'
                        ];
                        echo htmlspecialchars($access[$_SESSION['role']] ?? '제한됨');
                        ?>
                    </div>
                </div>
            </div>
        </div>

        <div class="info-section">
            <div class="section-title">빠른 작업</div>
            <div class="button-group">
                <button class="btn btn-primary">시스템 시작</button>
                <button class="btn btn-secondary">설정</button>
                <button class="btn btn-secondary">로그 보기</a>
                <a href="?logout=true" class="btn btn-secondary">로그아웃</a>
            </div>
        </div>
    </div>

    <script src="js/script.js"></script>
</body>
</html>
