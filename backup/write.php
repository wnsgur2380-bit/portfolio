<?php
session_start();

// 로그인 확인
if (!isset($_SESSION['user_id'])) {
    header('Location: login.php');
    exit;
}

$error = '';
$success = false;

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $title = $_POST['title'] ?? '';
    $content = $_POST['content'] ?? '';

    if (empty($title) || empty($content)) {
        $error = '제목과 내용을 입력하세요.';
    } else {
        $conn = new mysqli('localhost', 'root', '1234', 'web');
        
        if ($conn->connect_error) {
            $error = '데이터베이스 연결 실패';
        } else {
            $stmt = $conn->prepare('INSERT INTO posts (user_id, author, title, content) VALUES (?, ?, ?, ?)');
            $author = $_SESSION['full_name'];
            $stmt->bind_param('isss', $_SESSION['user_id'], $author, $title, $content);
            
            if ($stmt->execute()) {
                $success = true;
                $post_id = $conn->insert_id;
                header('Location: view.php?id=' . $post_id);
                exit;
            } else {
                $error = '글 저장 실패';
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
    <title>글쓰기</title>
    <link rel="stylesheet" href="css/style.css">
    <style>
        .write-section {
            min-height: 100vh;
            padding: 100px 20px 40px;
            background: #0a0a0a;
        }

        .write-container {
            max-width: 800px;
            margin: 0 auto;
        }

        .write-header {
            margin-bottom: 40px;
            padding-bottom: 20px;
            border-bottom: 2px solid #333333;
        }

        .write-header h1 {
            font-size: 32px;
            font-weight: 700;
            color: #ffffff;
            margin-bottom: 10px;
        }

        .write-header p {
            color: #b0b0b0;
        }

        .write-form {
            background: #1a1a1a;
            border: 1px solid #333333;
            border-radius: 10px;
            padding: 30px;
        }

        .form-group {
            margin-bottom: 25px;
        }

        .form-group label {
            display: block;
            color: #ffffff;
            margin-bottom: 10px;
            font-weight: 600;
            font-size: 14px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .form-group input,
        .form-group textarea {
            width: 100%;
            padding: 14px;
            border: 1px solid #333333;
            border-radius: 5px;
            background: #0d0d0d;
            color: #ffffff;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            font-size: 14px;
            transition: all 0.3s ease;
        }

        .form-group input:focus,
        .form-group textarea:focus {
            outline: none;
            border-color: #00d4ff;
            box-shadow: 0 0 0 3px rgba(0, 212, 255, 0.1);
        }

        .form-group textarea {
            resize: vertical;
            min-height: 400px;
        }

        .form-group input::placeholder,
        .form-group textarea::placeholder {
            color: #666666;
        }

        .error-message {
            background: rgba(255, 59, 59, 0.1);
            border: 1px solid #ff3b3b;
            color: #ff6b6b;
            padding: 14px;
            border-radius: 5px;
            margin-bottom: 20px;
            font-size: 14px;
        }

        .form-buttons {
            display: flex;
            gap: 10px;
            justify-content: flex-end;
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

        .btn-submit {
            background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
            color: #0a0a0a;
        }

        .btn-submit:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(0, 212, 255, 0.3);
        }

        .btn-cancel {
            background: transparent;
            color: #00d4ff;
            border: 1px solid #00d4ff;
        }

        .btn-cancel:hover {
            background: rgba(0, 212, 255, 0.1);
        }

        @media (max-width: 768px) {
            .write-section {
                padding: 80px 15px 30px;
            }

            .write-form {
                padding: 20px;
            }

            .form-buttons {
                flex-direction: column-reverse;
            }

            .btn {
                width: 100%;
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
                <li><a href="dashboard.php" class="nav-link contact-btn">대시보드</a></li>
                <li><a href="logout.php" class="nav-link contact-btn">로그아웃</a></li>
            </ul>
        </div>
    </nav>

    <section class="write-section">
        <div class="write-container">
            <div class="write-header">
                <h1>새 글 작성</h1>
                <p><?= htmlspecialchars($_SESSION['full_name']) ?>님</p>
            </div>

            <form method="POST" class="write-form">
                <?php if ($error): ?>
                    <div class="error-message"><?= htmlspecialchars($error) ?></div>
                <?php endif; ?>

                <div class="form-group">
                    <label for="title">제목</label>
                    <input 
                        type="text" 
                        id="title" 
                        name="title" 
                        placeholder="제목을 입력하세요"
                        required
                        autofocus
                    >
                </div>

                <div class="form-group">
                    <label for="content">내용</label>
                    <textarea 
                        id="content" 
                        name="content" 
                        placeholder="내용을 입력하세요"
                        required
                    ></textarea>
                </div>

                <div class="form-buttons">
                    <a href="board.php" class="btn btn-cancel">취소</a>
                    <button type="submit" class="btn btn-submit">게시</button>
                </div>
            </form>
        </div>
    </section>
</body>
</html>
