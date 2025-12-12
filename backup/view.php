<?php
session_start();

$post_id = $_GET['id'] ?? 0;

if (!$post_id) {
    header('Location: board.php');
    exit;
}

$conn = new mysqli('localhost', 'root', '1234', 'web');

if ($conn->connect_error) {
    die('데이터베이스 연결 실패');
}

// 조회수 증가
$conn->query('UPDATE posts SET views = views + 1 WHERE id = ' . intval($post_id));

// 게시글 조회
$result = $conn->query('SELECT p.id, p.title, p.content, u.full_name, p.created_at, p.views FROM posts p JOIN user u ON p.user_id = u.id WHERE p.id = ' . intval($post_id));

if ($result->num_rows === 0) {
    $conn->close();
    header('Location: board.php');
    exit;
}

$post = $result->fetch_assoc();

// 삭제 권한 확인
$can_delete = isset($_SESSION['user_id']) && isset($_SESSION['username']);
$is_author = isset($_SESSION['user_id']);

$conn->close();
?>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= htmlspecialchars($post['title']) ?></title>
    <link rel="stylesheet" href="css/style.css">
    <style>
        .view-section {
            min-height: 100vh;
            padding: 100px 20px 40px;
            background: #0a0a0a;
        }

        .view-container {
            max-width: 800px;
            margin: 0 auto;
        }

        .post-header {
            background: #1a1a1a;
            border: 1px solid #333333;
            border-radius: 10px 10px 0 0;
            padding: 30px;
            border-bottom: 2px solid #00d4ff;
        }

        .post-title {
            font-size: 28px;
            font-weight: 700;
            color: #ffffff;
            margin-bottom: 20px;
        }

        .post-meta {
            display: flex;
            gap: 20px;
            color: #b0b0b0;
            font-size: 13px;
            flex-wrap: wrap;
        }

        .post-meta-item {
            display: flex;
            align-items: center;
            gap: 5px;
        }

        .post-content {
            background: #1a1a1a;
            border: 1px solid #333333;
            padding: 40px 30px;
            color: #e0e0e0;
            line-height: 1.8;
            font-size: 15px;
            white-space: pre-wrap;
            word-wrap: break-word;
        }

        .post-footer {
            background: #1a1a1a;
            border: 1px solid #333333;
            border-radius: 0 0 10px 10px;
            padding: 20px 30px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .post-buttons {
            display: flex;
            gap: 10px;
        }

        .btn {
            padding: 10px 20px;
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

        .btn-danger {
            background: transparent;
            color: #ff6b6b;
            border: 1px solid #ff6b6b;
        }

        .btn-danger:hover {
            background: rgba(255, 107, 107, 0.1);
        }

        .btn:disabled {
            opacity: 0.5;
            cursor: not-allowed;
        }

        .back-link {
            color: #00d4ff;
            text-decoration: none;
            font-weight: 600;
            margin-bottom: 20px;
            display: inline-block;
            transition: color 0.3s ease;
        }

        .back-link:hover {
            color: #ffffff;
        }

        @media (max-width: 768px) {
            .view-section {
                padding: 80px 15px 30px;
            }

            .post-header,
            .post-content,
            .post-footer {
                padding: 20px;
            }

            .post-title {
                font-size: 22px;
            }

            .post-meta {
                flex-direction: column;
                gap: 5px;
            }

            .post-footer {
                flex-direction: column;
                gap: 15px;
                align-items: flex-start;
            }

            .post-buttons {
                width: 100%;
            }

            .btn {
                flex: 1;
                text-align: center;
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

    <section class="view-section">
        <div class="view-container">
            <a href="board.php" class="back-link">← 게시판으로 돌아가기</a>

            <div class="post-header">
                <h1 class="post-title"><?= htmlspecialchars($post['title']) ?></h1>
                <div class="post-meta">
                    <div class="post-meta-item">
                        <span>작성자:</span>
                        <strong><?= htmlspecialchars($post['full_name']) ?></strong>
                    </div>
                    <div class="post-meta-item">
                        <span>작성일:</span>
                        <strong><?= date('Y-m-d H:i', strtotime($post['created_at'])) ?></strong>
                    </div>
                    <div class="post-meta-item">
                        <span>조회수:</span>
                        <strong><?= $post['views'] ?></strong>
                    </div>
                </div>
            </div>

            <div class="post-content">
                <?= htmlspecialchars($post['content']) ?>
            </div>

            <div class="post-footer">
                <div></div>
                <div class="post-buttons">
                    <?php if ($is_author): ?>
                        <a href="edit.php?id=<?= $post['id'] ?>" class="btn btn-primary">수정</a>
                        <form method="POST" action="delete.php" style="display: inline;">
                            <input type="hidden" name="id" value="<?= $post['id'] ?>">
                            <button type="submit" class="btn btn-danger" onclick="return confirm('정말 삭제하시겠습니까?')">삭제</button>
                        </form>
                    <?php endif; ?>
                </div>
            </div>
        </div>
    </section>
</body>
</html>
