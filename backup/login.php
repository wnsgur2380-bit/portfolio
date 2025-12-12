<?php
session_start();

// 이미 로그인한 경우 대시보드로 이동
if (isset($_SESSION['user_id'])) {
    header('Location: dashboard.php');
    exit;
}

$error = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = $_POST['username'] ?? '';
    $password = $_POST['password'] ?? '';

    if (empty($username) || empty($password)) {
        $error = '사용자명과 비밀번호를 입력하세요.';
    } else {
        // 데이터베이스 연결
        $conn = new mysqli('localhost', 'root', '1234', 'web');

        if ($conn->connect_error) {
            $error = '데이터베이스 연결 실패';
        } else {
            // 사용자 조회
            $stmt = $conn->prepare('SELECT id, username, password, full_name, role FROM user WHERE username = ?');
            $stmt->bind_param('s', $username);
            $stmt->execute();
            $result = $stmt->get_result();

            if ($result->num_rows === 1) {
                $user = $result->fetch_assoc();

                // 비밀번호 검증
                if (password_verify($password, $user['password'])) {
                    // 세션 설정
                    $_SESSION['user_id'] = $user['id'];
                    $_SESSION['username'] = $user['username'];
                    $_SESSION['full_name'] = $user['full_name'];
                    $_SESSION['role'] = $user['role'];

                    // 마지막 로그인 시간 업데이트
                    $update_stmt = $conn->prepare('UPDATE user SET last_login = NOW() WHERE id = ?');
                    $update_stmt->bind_param('i', $user['id']);
                    $update_stmt->execute();
                    $update_stmt->close();

                    header('Location: dashboard.php');
                    exit;
                } else {
                    $error = '비밀번호가 일치하지 않습니다.';
                }
            } else {
                $error = '사용자명이 존재하지 않습니다.';
            }

            $stmt->close();
            $conn->close();
        }
    }
}
?>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>로그인</title>
    <link rel="stylesheet" href="css/style.css">
    <style>
        .login-section {
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
            margin-top: 70px;
        }

        .login-container {
            width: 100%;
            max-width: 450px;
        }

        .login-header {
            text-align: center;
            margin-bottom: 50px;
        }

        .login-header h1 {
            font-size: 42px;
            font-weight: 700;
            margin-bottom: 15px;
            background: linear-gradient(135deg, #ffffff 0%, #00d4ff 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
        }

        .login-header p {
            color: #b0b0b0;
            font-size: 16px;
            letter-spacing: 1px;
        }

        .login-form {
            background: #1a1a1a;
            border: 1px solid #333333;
            border-radius: 10px;
            padding: 50px 40px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
        }

        .form-group {
            margin-bottom: 25px;
        }

        .form-group label {
            display: block;
            color: #ffffff;
            margin-bottom: 12px;
            font-weight: 600;
            font-size: 14px;
            text-transform: uppercase;
            letter-spacing: 1px;
        }

        .form-group input {
            width: 100%;
            padding: 14px 16px;
            border: 1px solid #333333;
            border-radius: 5px;
            background: #0d0d0d;
            color: #ffffff;
            font-size: 15px;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            transition: all 0.3s ease;
        }

        .form-group input:focus {
            outline: none;
            border-color: #00d4ff;
            box-shadow: 0 0 0 3px rgba(0, 212, 255, 0.1);
            background: #111111;
        }

        .form-group input::placeholder {
            color: #666666;
        }

        .error-message {
            background: rgba(255, 59, 59, 0.1);
            border: 1px solid #ff3b3b;
            color: #ff6b6b;
            padding: 14px 16px;
            border-radius: 5px;
            margin-bottom: 20px;
            font-size: 14px;
            text-align: center;
        }

        .login-button {
            width: 100%;
            padding: 14px;
            background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
            color: #0a0a0a;
            border: none;
            border-radius: 5px;
            font-size: 15px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 1px;
            cursor: pointer;
            transition: all 0.3s ease;
            margin-top: 10px;
        }

        .login-button:hover {
            transform: translateY(-3px);
            box-shadow: 0 10px 30px rgba(0, 212, 255, 0.3);
        }

        .login-button:active {
            transform: translateY(-1px);
        }

        .login-footer {
            text-align: center;
            margin-top: 30px;
            color: #666666;
            font-size: 13px;
        }

        .login-footer a {
            color: #00d4ff;
            text-decoration: none;
            transition: color 0.3s ease;
        }

        .login-footer a:hover {
            color: #ffffff;
        }

        .divider {
            height: 1px;
            background: #333333;
            margin: 30px 0;
        }

        .demo-accounts {
            text-align: center;
            padding-top: 20px;
        }

        .demo-accounts p {
            color: #b0b0b0;
            font-size: 13px;
            margin-bottom: 12px;
        }

        .account-list {
            display: grid;
            grid-template-columns: 1fr;
            gap: 8px;
            text-align: left;
        }

        .account-item {
            background: #0d0d0d;
            padding: 10px 12px;
            border-radius: 5px;
            border-left: 3px solid #00d4ff;
            font-size: 12px;
            color: #b0b0b0;
        }

        .account-item code {
            color: #00d4ff;
            font-weight: 600;
        }

        /* Navbar 숨김 */
        .navbar {
            display: none;
        }

        /* 반응형 디자인 */
        @media (max-width: 768px) {
            .login-section {
                margin-top: 0;
            }

            .login-form {
                padding: 40px 25px;
            }

            .login-header h1 {
                font-size: 32px;
            }

            .login-header {
                margin-bottom: 40px;
            }
        }
    </style>
</head>
<body>
    <section class="login-section">
        <div class="login-container">
            <div class="login-header">
                <h1>System Access</h1>
                <p>로그인하여 시스템에 접근하세요</p>
            </div>

            <form method="POST" class="login-form">
                <?php if ($error): ?>
                    <div class="error-message"><?= htmlspecialchars($error) ?></div>
                <?php endif; ?>

                <div class="form-group">
                    <label for="username">사용자명</label>
                    <input 
                        type="text" 
                        id="username" 
                        name="username" 
                        placeholder="사용자명을 입력하세요"
                        required
                        autofocus
                    >
                </div>

                <div class="form-group">
                    <label for="password">비밀번호</label>
                    <input 
                        type="password" 
                        id="password" 
                        name="password" 
                        placeholder="비밀번호를 입력하세요"
                        required
                    >
                </div>

                <button type="submit" class="login-button">로그인</button>
            </form>

            <div class="login-footer">
                <a href="index.html">← 홈페이지로 돌아가기</a> | <a href="board.php">게시판 보기</a>
            </div>

            <div class="divider"></div>

            <div class="demo-accounts">
                <p><strong>테스트 계정</strong></p>
                <div class="account-list">
                    <div class="account-item">
                        <code>admin</code> / <code>admin123</code>
                    </div>
                    <div class="account-item">
                        <code>operator</code> / <code>admin123</code>
                    </div>
                    <div class="account-item">
                        <code>viewer</code> / <code>admin123</code>
                    </div>
                </div>
            </div>
        </div>
    </section>
</body>
</html>
