<?php
session_start();
?>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>게시판</title>
    <link rel="stylesheet" href="css/style.css">
    <style>
        .board-section {
            min-height: 100vh;
            padding: 100px 20px 40px;
            background: #0a0a0a;
        }

        .board-container {
            max-width: 1000px;
            margin: 0 auto;
        }

        .board-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 40px;
            padding-bottom: 20px;
            border-bottom: 2px solid #333333;
        }

        .board-header h1 {
            font-size: 32px;
            font-weight: 700;
            color: #ffffff;
        }

        .board-header span {
            color: #00d4ff;
        }

        .write-btn {
            padding: 12px 24px;
            background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
            color: #0a0a0a;
            border: none;
            border-radius: 5px;
            font-weight: 600;
            cursor: pointer;
            text-decoration: none;
            transition: all 0.3s ease;
            display: inline-block;
        }

        .write-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(0, 212, 255, 0.3);
        }

        .write-btn:disabled {
            background: #666;
            cursor: not-allowed;
            opacity: 0.5;
        }

        .board-list {
            background: #1a1a1a;
            border: 1px solid #333333;
            border-radius: 10px;
            overflow: hidden;
        }

        .board-item {
            display: grid;
            grid-template-columns: 1fr 150px 100px 100px;
            gap: 20px;
            padding: 20px;
            border-bottom: 1px solid #333333;
            align-items: center;
            transition: all 0.3s ease;
            cursor: pointer;
        }

        .board-item:last-child {
            border-bottom: none;
        }

        .board-item:hover {
            background: #242424;
        }

        .board-item-title {
            color: #ffffff;
            font-weight: 600;
            font-size: 15px;
        }

        .board-item-title:hover {
            color: #00d4ff;
        }

        .board-item-author {
            color: #b0b0b0;
            font-size: 13px;
        }

        .board-item-date {
            color: #666666;
            font-size: 13px;
            text-align: center;
        }

        .board-item-views {
            color: #666666;
            font-size: 13px;
            text-align: right;
        }

        .board-header-row {
            display: grid;
            grid-template-columns: 1fr 150px 100px 100px;
            gap: 20px;
            padding: 15px 20px;
            background: #0d0d0d;
            border-bottom: 2px solid #00d4ff;
            font-weight: 600;
            color: #00d4ff;
            font-size: 13px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .empty-message {
            text-align: center;
            padding: 60px 20px;
            color: #666666;
        }

        .empty-message p {
            font-size: 16px;
            margin-bottom: 20px;
        }

        .login-prompt {
            text-align: center;
            padding: 20px;
            background: rgba(0, 212, 255, 0.1);
            border: 1px solid #00d4ff;
            border-radius: 5px;
            margin-bottom: 30px;
        }

        .login-prompt p {
            color: #ffffff;
            margin-bottom: 10px;
        }

        .login-prompt a {
            color: #00d4ff;
            text-decoration: none;
            font-weight: 600;
            transition: color 0.3s ease;
        }

        .login-prompt a:hover {
            color: #ffffff;
        }

        @media (max-width: 768px) {
            .board-header {
                flex-direction: column;
                gap: 15px;
                align-items: flex-start;
            }

            .board-item,
            .board-header-row {
                grid-template-columns: 1fr;
                gap: 10px;
            }

            .board-item-date,
            .board-item-views,
            .board-header-row span:nth-child(2),
            .board-header-row span:nth-child(3),
            .board-header-row span:nth-child(4) {
                display: none;
            }
        }
    </style>
</head>
<body>
    <!-- Navigation -->
    <nav class="navbar">
        <div class="nav-container">
            <div class="logo">
                <a href="index.html">Myworks <span>*</span></a>
            </div>
            <ul class="nav-menu">
                <li><a href="index.html#about" class="nav-link">자기소개</a></li>
                <li><a href="index.html#portfolio" class="nav-link">포트폴리오</a></li>
                <li><a href="index.html#career" class="nav-link">직업소개</a></li>
                <li><a href="index.html#projects" class="nav-link">프로젝트</a></li>
                <li><a href="board.php" class="nav-link">게시판</a></li>
                <?php if (isset($_SESSION['user_id'])): ?>
                    <li><a href="dashboard.php" class="nav-link contact-btn">대시보드</a></li>
                    <li><a href="logout.php" class="nav-link contact-btn">로그아웃</a></li>
                <?php else: ?>
                    <li><a href="login.php" class="nav-link contact-btn">로그인</a></li>
                <?php endif; ?>
            </ul>
        </div>
    </nav>

    <section class="board-section">
        <div class="board-container">
            <div class="board-header">
                <h1>게시판</h1>
                <?php if (isset($_SESSION['user_id'])): ?>
                    <a href="write.php" class="write-btn">글쓰기</a>
                <?php else: ?>
                    <button class="write-btn" disabled title="로그인 후 사용 가능">글쓰기</button>
                <?php endif; ?>
            </div>

            <?php if (!isset($_SESSION['user_id'])): ?>
                <div class="login-prompt">
                    <p>게시글을 작성하려면 로그인이 필요합니다.</p>
                    <a href="login.php">로그인하기</a>
                </div>
            <?php endif; ?>

            <div class="board-list">
                <div class="board-header-row">
                    <span>제목</span>
                    <span>작성자</span>
                    <span>작성일</span>
                    <span>조회수</span>
                </div>

                <?php
                $conn = new mysqli('localhost', 'root', '1234', 'web');
                
                if ($conn->connect_error) {
                    echo '<div class="empty-message"><p>데이터베이스 연결 실패</p></div>';
                } else {
                    $result = $conn->query('SELECT p.id, p.title, u.full_name, p.created_at, p.views FROM posts p JOIN user u ON p.user_id = u.id ORDER BY p.created_at DESC');
                    
                    if ($result->num_rows > 0) {
                        while ($post = $result->fetch_assoc()) {
                            $date = date('Y-m-d', strtotime($post['created_at']));
                            echo '<div class="board-item" onclick="location.href=\'view.php?id=' . $post['id'] . '\';">';
                            echo '<div class="board-item-title">' . htmlspecialchars($post['title']) . '</div>';
                            echo '<div class="board-item-author">' . htmlspecialchars($post['full_name']) . '</div>';
                            echo '<div class="board-item-date">' . $date . '</div>';
                            echo '<div class="board-item-views">' . $post['views'] . '</div>';
                            echo '</div>';
                        }
                    } else {
                        echo '<div class="empty-message"><p>게시글이 없습니다</p></div>';
                    }
                    $conn->close();
                }
                ?>
            </div>
        </div>
    </section>
</body>
</html>
