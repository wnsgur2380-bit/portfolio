const express = require('express');
const session = require('express-session');
const bodyParser = require('body-parser');
const mysql = require('mysql2/promise');
const path = require('path');
const bcrypt = require('bcrypt');
const http = require('http');
const WebSocket = require('ws');

const app = express();
const PORT = 3000;

// HTTP 서버 생성
const server = http.createServer(app);

// WebSocket 서버 생성
const wss = new WebSocket.Server({ server });

// 접속한 채팅 클라이언트 저장
const chatClients = new Map();

// Database connection pool
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '1234',
    database: 'web',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

// Middleware
app.use(bodyParser.urlencoded({ extended: true, limit: '100mb' }));
app.use(bodyParser.json({ limit: '100mb' }));

app.use(session({
    secret: 'your-secret-key-change-this',
    resave: false,
    saveUninitialized: true,
    cookie: { 
        maxAge: 24 * 60 * 60 * 1000,
        secure: process.env.NODE_ENV === 'production',  // HTTPS에서만 쿠키 전송
        sameSite: 'lax'  // CORS 요청에서도 쿠키 전송
    }
}));

// Static files
app.use(express.static(path.join(__dirname, 'public')));

// Routes
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.get('/login', (req, res) => {
    if (req.session.user_id) {
        return res.redirect('/dashboard');
    }
    res.sendFile(path.join(__dirname, 'public', 'login.html'));
});

app.get('/signup', (req, res) => {
    if (req.session.user_id) {
        return res.redirect('/dashboard');
    }
    res.sendFile(path.join(__dirname, 'public', 'signup.html'));
});

app.post('/signup', async (req, res) => {
    const { username, password, confirmPassword, fullName } = req.body;

    // Validation
    if (!username || !password || !confirmPassword || !fullName) {
        return res.status(400).json({ error: '모든 필드를 입력하세요.' });
    }

    if (username.length < 3) {
        return res.status(400).json({ error: '사용자명은 3글자 이상이어야 합니다.' });
    }

    if (password.length < 6) {
        return res.status(400).json({ error: '비밀번호는 6글자 이상이어야 합니다.' });
    }

    if (password !== confirmPassword) {
        return res.status(400).json({ error: '비밀번호가 일치하지 않습니다.' });
    }

    try {
        const conn = await pool.getConnection();
        
        // Check if username already exists
        const [existingUsers] = await conn.query(
            'SELECT id FROM user WHERE username = ?',
            [username]
        );

        if (existingUsers.length > 0) {
            conn.release();
            return res.status(400).json({ error: '이미 사용 중인 사용자명입니다.' });
        }

        // Hash password
        const hashedPassword = await bcrypt.hash(password, 10);

        // Create new user
        await conn.query(
            'INSERT INTO user (username, password, full_name, role, is_active) VALUES (?, ?, ?, ?, ?)',
            [username, hashedPassword, fullName, 'viewer', 1]
        );

        conn.release();

        res.json({ success: true, message: '회원가입이 완료되었습니다. 로그인하세요.' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '회원가입 처리 중 오류가 발생했습니다.' });
    }
});

app.post('/login', async (req, res) => {
    const { username, password } = req.body;

    if (!username || !password) {
        return res.status(400).json({ error: '사용자명과 비밀번호를 입력하세요.' });
    }

    try {
        const conn = await pool.getConnection();
        const [users] = await conn.query(
            'SELECT id, username, password, full_name, role FROM user WHERE username = ?',
            [username]
        );
        conn.release();

        if (users.length === 0) {
            return res.status(401).json({ error: '사용자명이 존재하지 않습니다.' });
        }

        const user = users[0];
        
        // Password verification - check if stored password is bcrypt hash or plain text
        let passwordMatch = user.password === password;
        
        // If not plain text match, try bcrypt
        if (!passwordMatch && user.password.startsWith('$2')) {
            passwordMatch = await bcrypt.compare(password, user.password);
        }
        
        if (!passwordMatch) {
            return res.status(401).json({ error: '비밀번호가 일치하지 않습니다.' });
        }

        // Set session
        req.session.user_id = user.id;
        req.session.username = user.username;
        req.session.full_name = user.full_name;
        req.session.role = user.role;

        // Update last login
        const conn2 = await pool.getConnection();
        await conn2.query('UPDATE user SET last_login = NOW() WHERE id = ?', [user.id]);
        conn2.release();

        res.json({ success: true, redirect: '/dashboard' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '로그인 처리 중 오류가 발생했습니다.' });
    }
});

app.get('/dashboard', (req, res) => {
    if (!req.session.user_id) {
        return res.redirect('/login');
    }
    res.sendFile(path.join(__dirname, 'public', 'dashboard.html'));
});

app.get('/profile', (req, res) => {
    if (!req.session.user_id) {
        return res.redirect('/login');
    }
    res.sendFile(path.join(__dirname, 'public', 'profile.html'));
});

app.get('/board', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'board.html'));
});

app.get('/board/view/:id', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'post-view.html'));
});

// Chat route (login required)
app.get('/chat', (req, res) => {
    if (!req.session.user_id) {
        return res.redirect('/login');
    }
    res.sendFile(path.join(__dirname, 'public', 'chat.html'));
});

app.post('/logout', (req, res) => {
    req.session.destroy((err) => {
        if (err) {
            return res.status(500).json({ error: '로그아웃 중 오류가 발생했습니다.' });
        }
        res.json({ success: true, redirect: '/' });
    });
});

app.get('/api/status', (req, res) => {
    res.json({ 
        isLoggedIn: req.session.user_id ? true : false,
        loggedIn: req.session.user_id ? true : false,
        username: req.session.username || null,
        role: req.session.role || null
    });
});

app.get('/api/user-info', (req, res) => {
    if (!req.session.user_id) {
        return res.status(401).json({ error: 'Not logged in' });
    }

    res.json({
        id: req.session.user_id,
        username: req.session.username,
        full_name: req.session.full_name,
        role: req.session.role
    });
});

// Update user info
app.put('/api/user-info', async (req, res) => {
    if (!req.session.user_id) {
        return res.status(401).json({ error: '로그인이 필요합니다.' });
    }

    const { full_name, current_password, new_password } = req.body;

    if (!full_name) {
        return res.status(400).json({ error: '이름을 입력하세요.' });
    }

    try {
        const conn = await pool.getConnection();

        // 비밀번호 변경 요청이 있으면 검증
        if (new_password) {
            if (!current_password) {
                return res.status(400).json({ error: '현재 비밀번호를 입력하세요.' });
            }

            // 현재 사용자 정보 조회
            const [users] = await conn.query(
                'SELECT password FROM user WHERE id = ?',
                [req.session.user_id]
            );

            if (users.length === 0) {
                conn.release();
                return res.status(404).json({ error: '사용자를 찾을 수 없습니다.' });
            }

            // 현재 비밀번호 확인
            const isPasswordValid = await bcrypt.compare(current_password, users[0].password);
            if (!isPasswordValid) {
                conn.release();
                return res.status(400).json({ error: '현재 비밀번호가 일치하지 않습니다.' });
            }

            // 새 비밀번호 해시
            const hashedPassword = await bcrypt.hash(new_password, 10);
            await conn.query(
                'UPDATE user SET full_name = ?, password = ? WHERE id = ?',
                [full_name, hashedPassword, req.session.user_id]
            );
        } else {
            // 비밀번호 변경 없이 정보만 수정
            await conn.query(
                'UPDATE user SET full_name = ? WHERE id = ?',
                [full_name, req.session.user_id]
            );
        }

        conn.release();

        // Update session
        req.session.full_name = full_name;

        res.json({ success: true, message: '사용자 정보가 수정되었습니다.' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '사용자 정보 수정 중 오류가 발생했습니다.' });
    }
});

// Delete user account
app.delete('/api/user-info', async (req, res) => {
    if (!req.session.user_id) {
        return res.status(401).json({ error: '로그인이 필요합니다.' });
    }

    try {
        const conn = await pool.getConnection();
        
        // Delete user (cascade will handle posts and comments)
        await conn.query('DELETE FROM user WHERE id = ?', [req.session.user_id]);
        conn.release();

        // Destroy session
        req.session.destroy();

        res.json({ success: true, message: '계정이 삭제되었습니다.' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '계정 삭제 중 오류가 발생했습니다.' });
    }
});

// Board API - Get all posts
app.get('/api/posts', async (req, res) => {
    try {
        const conn = await pool.getConnection();
        const [posts] = await conn.query(
            `SELECT p.id, p.title, u.username, u.full_name, p.content, p.views, 
                    p.created_at, p.updated_at
             FROM post p
             LEFT JOIN user u ON p.user_id = u.id
             ORDER BY p.created_at DESC`
        );
        conn.release();

        res.json(posts);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '포스트 조회 중 오류가 발생했습니다.' });
    }
});

// Board API - Get single post
app.get('/api/posts/:id', async (req, res) => {
    const { id } = req.params;

    try {
        const conn = await pool.getConnection();

        // Increment views
        await conn.query('UPDATE post SET views = views + 1 WHERE id = ?', [id]);

        // Get post
        const [posts] = await conn.query(
            `SELECT p.*, u.username, u.full_name FROM post p
             LEFT JOIN user u ON p.user_id = u.id
             WHERE p.id = ?`,
            [id]
        );
        conn.release();

        if (posts.length === 0) {
            return res.status(404).json({ error: '포스트를 찾을 수 없습니다.' });
        }

        res.json(posts[0]);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '포스트 조회 중 오류가 발생했습니다.' });
    }
});

// Board API - Create post (login required)
app.post('/api/posts', async (req, res) => {
    if (!req.session.user_id) {
        return res.status(401).json({ error: '로그인이 필요합니다.' });
    }

    const { title, content } = req.body;

    if (!title || !content) {
        return res.status(400).json({ error: '제목과 내용을 입력하세요.' });
    }

    try {
        const conn = await pool.getConnection();
        await conn.query(
            `INSERT INTO post (title, content, user_id, created_at, updated_at)
             VALUES (?, ?, ?, NOW(), NOW())`,
            [title, content, req.session.user_id]
        );
        conn.release();

        res.json({ success: true, message: '포스트가 생성되었습니다.' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '포스트 생성 중 오류가 발생했습니다.' });
    }
});

// Board API - Update post (author only)
app.put('/api/posts/:id', async (req, res) => {
    const { id } = req.params;
    const { title, content } = req.body;

    if (!req.session.user_id) {
        return res.status(401).json({ error: '로그인이 필요합니다.' });
    }

    try {
        const conn = await pool.getConnection();

        // Check if user is author
        const [posts] = await conn.query(
            'SELECT user_id FROM post WHERE id = ?',
            [id]
        );

        if (posts.length === 0) {
            conn.release();
            return res.status(404).json({ error: '포스트를 찾을 수 없습니다.' });
        }

        if (posts[0].user_id !== req.session.user_id) {
            conn.release();
            return res.status(403).json({ error: '수정 권한이 없습니다.' });
        }

        await conn.query(
            'UPDATE post SET title = ?, content = ?, updated_at = NOW() WHERE id = ?',
            [title, content, id]
        );
        conn.release();

        res.json({ success: true, message: '포스트가 수정되었습니다.' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '포스트 수정 중 오류가 발생했습니다.' });
    }
});

// Board API - Delete post (author only)
app.delete('/api/posts/:id', async (req, res) => {
    const { id } = req.params;

    if (!req.session.user_id) {
        return res.status(401).json({ error: '로그인이 필요합니다.' });
    }

    try {
        const conn = await pool.getConnection();

        // Check if user is author
        const [posts] = await conn.query(
            'SELECT user_id FROM post WHERE id = ?',
            [id]
        );

        if (posts.length === 0) {
            conn.release();
            return res.status(404).json({ error: '포스트를 찾을 수 없습니다.' });
        }

        if (posts[0].user_id !== req.session.user_id) {
            conn.release();
            return res.status(403).json({ error: '삭제 권한이 없습니다.' });
        }

        await conn.query('DELETE FROM post WHERE id = ?', [id]);
        conn.release();

        res.json({ success: true, message: '포스트가 삭제되었습니다.' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '포스트 삭제 중 오류가 발생했습니다.' });
    }
});

// Comments API - Get all comments for a post
app.get('/api/posts/:id/comments', async (req, res) => {
    const { id } = req.params;

    try {
        const conn = await pool.getConnection();
        const [comments] = await conn.query(
            `SELECT c.id, c.post_id, c.user_id, u.username, u.full_name, 
                    c.content, c.created_at, c.updated_at
             FROM comment c
             JOIN user u ON c.user_id = u.id
             WHERE c.post_id = ?
             ORDER BY c.created_at ASC`,
            [id]
        );
        conn.release();

        res.json(comments);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '댓글 조회 중 오류가 발생했습니다.' });
    }
});

// Comments API - Create comment
app.post('/api/posts/:id/comments', async (req, res) => {
    const { id } = req.params;
    const { content } = req.body;

    if (!req.session.user_id) {
        return res.status(401).json({ error: '로그인이 필요합니다.' });
    }

    if (!content || content.trim() === '') {
        return res.status(400).json({ error: '댓글 내용을 입력하세요.' });
    }

    try {
        const conn = await pool.getConnection();
        
        // Check if post exists
        const [posts] = await conn.query('SELECT id FROM post WHERE id = ?', [id]);
        if (posts.length === 0) {
            conn.release();
            return res.status(404).json({ error: '포스트를 찾을 수 없습니다.' });
        }

        await conn.query(
            'INSERT INTO comment (post_id, user_id, content, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())',
            [id, req.session.user_id, content]
        );
        conn.release();

        res.json({ success: true, message: '댓글이 작성되었습니다.' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '댓글 작성 중 오류가 발생했습니다.' });
    }
});

// Comments API - Update comment (author only)
app.put('/api/comments/:id', async (req, res) => {
    const { id } = req.params;
    const { content } = req.body;

    if (!req.session.user_id) {
        return res.status(401).json({ error: '로그인이 필요합니다.' });
    }

    if (!content || content.trim() === '') {
        return res.status(400).json({ error: '댓글 내용을 입력하세요.' });
    }

    try {
        const conn = await pool.getConnection();

        // Check if comment exists and user is author
        const [comments] = await conn.query(
            'SELECT user_id FROM comment WHERE id = ?',
            [id]
        );

        if (comments.length === 0) {
            conn.release();
            return res.status(404).json({ error: '댓글을 찾을 수 없습니다.' });
        }

        if (comments[0].user_id !== req.session.user_id) {
            conn.release();
            return res.status(403).json({ error: '수정 권한이 없습니다.' });
        }

        await conn.query(
            'UPDATE comment SET content = ?, updated_at = NOW() WHERE id = ?',
            [content, id]
        );
        conn.release();

        res.json({ success: true, message: '댓글이 수정되었습니다.' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '댓글 수정 중 오류가 발생했습니다.' });
    }
});

// Comments API - Delete comment (author only)
app.delete('/api/comments/:id', async (req, res) => {
    const { id } = req.params;

    if (!req.session.user_id) {
        return res.status(401).json({ error: '로그인이 필요합니다.' });
    }

    try {
        const conn = await pool.getConnection();

        // Check if comment exists and user is author
        const [comments] = await conn.query(
            'SELECT user_id FROM comment WHERE id = ?',
            [id]
        );

        if (comments.length === 0) {
            conn.release();
            return res.status(404).json({ error: '댓글을 찾을 수 없습니다.' });
        }

        if (comments[0].user_id !== req.session.user_id) {
            conn.release();
            return res.status(403).json({ error: '삭제 권한이 없습니다.' });
        }

        await conn.query('DELETE FROM comment WHERE id = ?', [id]);
        conn.release();

        res.json({ success: true, message: '댓글이 삭제되었습니다.' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '댓글 삭제 중 오류가 발생했습니다.' });
    }
});

// Admin page route
app.get('/admin', (req, res) => {
    if (!req.session.user_id) {
        return res.redirect('/login');
    }
    if (req.session.role !== 'admin') {
        return res.status(403).send('관리자만 접근 가능합니다.');
    }
    res.sendFile(path.join(__dirname, 'public', 'admin.html'));
});

// Admin API - Get all users
app.get('/api/admin/users', async (req, res) => {
    if (!req.session.user_id || req.session.role !== 'admin') {
        return res.status(403).json({ error: '관리자만 접근 가능합니다.' });
    }

    try {
        const conn = await pool.getConnection();
        const [users] = await conn.query(
            'SELECT id, username, full_name, role, is_active, created_at, last_login FROM user ORDER BY created_at DESC'
        );
        conn.release();

        res.json(users);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '사용자 조회 중 오류가 발생했습니다.' });
    }
});

// Admin API - Update user role
app.put('/api/admin/users/:id', async (req, res) => {
    if (!req.session.user_id || req.session.role !== 'admin') {
        return res.status(403).json({ error: '관리자만 접근 가능합니다.' });
    }

    const { id } = req.params;
    const { full_name, role, is_active } = req.body;

    if (!full_name) {
        return res.status(400).json({ error: '이름을 입력하세요.' });
    }

    try {
        const conn = await pool.getConnection();
        await conn.query(
            'UPDATE user SET full_name = ?, role = ?, is_active = ? WHERE id = ?',
            [full_name, role, is_active, id]
        );
        conn.release();

        res.json({ success: true, message: '사용자 정보가 업데이트되었습니다.' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '사용자 업데이트 중 오류가 발생했습니다.' });
    }
});

// Admin API - Delete user
app.delete('/api/admin/users/:id', async (req, res) => {
    if (!req.session.user_id || req.session.role !== 'admin') {
        return res.status(403).json({ error: '관리자만 접근 가능합니다.' });
    }

    const { id } = req.params;

    try {
        const conn = await pool.getConnection();
        await conn.query('DELETE FROM user WHERE id = ?', [id]);
        conn.release();

        res.json({ success: true, message: '사용자가 삭제되었습니다.' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '사용자 삭제 중 오류가 발생했습니다.' });
    }
});

// Admin API - Get all posts
app.get('/api/admin/posts', async (req, res) => {
    if (!req.session.user_id || req.session.role !== 'admin') {
        return res.status(403).json({ error: '관리자만 접근 가능합니다.' });
    }

    try {
        const conn = await pool.getConnection();
        const [posts] = await conn.query(
            `SELECT p.id, p.title, p.user_id, u.username, u.full_name, p.created_at, p.updated_at, p.views
             FROM post p
             LEFT JOIN user u ON p.user_id = u.id
             ORDER BY p.created_at DESC`
        );
        conn.release();

        res.json(posts);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '글 조회 중 오류가 발생했습니다.' });
    }
});

// Admin API - Delete post
app.delete('/api/admin/posts/:id', async (req, res) => {
    if (!req.session.user_id || req.session.role !== 'admin') {
        return res.status(403).json({ error: '관리자만 접근 가능합니다.' });
    }

    const { id } = req.params;

    try {
        const conn = await pool.getConnection();
        await conn.query('DELETE FROM post WHERE id = ?', [id]);
        conn.release();

        res.json({ success: true, message: '글이 삭제되었습니다.' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '글 삭제 중 오류가 발생했습니다.' });
    }
});

// Admin API - Get all comments
app.get('/api/admin/comments', async (req, res) => {
    if (!req.session.user_id || req.session.role !== 'admin') {
        return res.status(403).json({ error: '관리자만 접근 가능합니다.' });
    }

    try {
        const conn = await pool.getConnection();
        const [comments] = await conn.query(
            `SELECT c.id, c.content, c.user_id, c.post_id, u.username, u.full_name, p.title as post_title, c.created_at
             FROM comment c
             LEFT JOIN user u ON c.user_id = u.id
             LEFT JOIN post p ON c.post_id = p.id
             ORDER BY c.created_at DESC`
        );
        conn.release();

        res.json(comments);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '댓글 조회 중 오류가 발생했습니다.' });
    }
});

// Admin API - Delete comment
app.delete('/api/admin/comments/:id', async (req, res) => {
    if (!req.session.user_id || req.session.role !== 'admin') {
        return res.status(403).json({ error: '관리자만 접근 가능합니다.' });
    }

    const { id } = req.params;

    try {
        const conn = await pool.getConnection();
        await conn.query('DELETE FROM comment WHERE id = ?', [id]);
        conn.release();

        res.json({ success: true, message: '댓글이 삭제되었습니다.' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '댓글 삭제 중 오류가 발생했습니다.' });
    }
});

// WebSocket 핸들러 - 채팅
wss.on('connection', (ws) => {
    const clientId = Date.now().toString();
    const clientInfo = {
        ws: ws,
        nickname: null,
        id: clientId
    };

    // 새 클라이언트 정보 저장
    chatClients.set(clientId, clientInfo);
    console.log(`[채팅] 클라이언트 연결: ${clientId}`);

    // 메시지 수신
    ws.on('message', (msg) => {
        try {
            const data = JSON.parse(msg);

            // 닉네임 설정
            if (data.type === 'nickname') {
                clientInfo.nickname = data.nickname;
                console.log(`[채팅] 닉네임 설정: ${clientId}: ${data.nickname}`);

                // 모든 클라이언트에게 사용자 수 전송
                broadcastUserCount();

                // 입장 메시지 브로드캐스트
                broadcast({
                    type: 'userJoined',
                    nickname: data.nickname
                }, clientId);
            }
            // 일반 메시지
            else if (data.type === 'message') {
                console.log(`[채팅] 메시지: ${data.nickname}: ${data.message}`);

                // 모든 클라이언트에게 메시지 전송
                chatClients.forEach((client, id) => {
                    if (client.ws && client.ws.readyState === WebSocket.OPEN) {
                        client.ws.send(JSON.stringify({
                            type: 'message',
                            nickname: data.nickname,
                            message: data.message,
                            isMe: id === clientId
                        }));
                    }
                });
            }
        } catch (error) {
            console.error('[채팅] 메시지 처리 오류:', error);
        }
    });

    // 연결 종료
    ws.on('close', () => {
        const nickname = clientInfo.nickname;
        chatClients.delete(clientId);
        console.log(`[채팅] 클라이언트 종료: ${clientId}: ${nickname}`);

        if (nickname) {
            // 퇴장 메시지 브로드캐스트
            broadcast({
                type: 'userLeft',
                nickname: nickname
            });

            // 모든 클라이언트에게 사용자 수 전송
            broadcastUserCount();
        }
    });

    // 에러 처리
    ws.on('error', (error) => {
        console.error(`[채팅] WebSocket 에러 ${clientId}:`, error);
    });
});

// 모든 클라이언트에게 사용자 수 전송
function broadcastUserCount() {
    const count = chatClients.size;
    chatClients.forEach((client) => {
        if (client.ws && client.ws.readyState === WebSocket.OPEN) {
            client.ws.send(JSON.stringify({
                type: 'userCount',
                count: count
            }));
        }
    });
}

// 특정 클라이언트를 제외한 모든 클라이언트에게 메시지 전송
function broadcast(message, excludeId = null) {
    chatClients.forEach((client, id) => {
        if (client.ws && client.ws.readyState === WebSocket.OPEN && id !== excludeId) {
            client.ws.send(JSON.stringify(message));
        }
    });
}

server.listen(PORT, () => {
    console.log(`Server running on http://localhost:${PORT}`);
});

// Portfolio API - Get all portfolios (public)
app.get('/api/portfolio', async (req, res) => {
    try {
        const conn = await pool.getConnection();
        const [portfolios] = await conn.query(
            `SELECT id, title, description, image_path as image, project_url, tags
             FROM portfolio_item 
             ORDER BY id ASC`
        );
        conn.release();

        // Parse tags JSON with error handling
        const result = portfolios.map(p => ({
            ...p,
            tags: p.tags ? (() => {
                try {
                    const parsed = typeof p.tags === 'string' ? JSON.parse(p.tags) : p.tags;
                    return Array.isArray(parsed) ? parsed : [];
                } catch (e) {
                    return [];
                }
            })() : []
        }));

        res.json(result);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '포트폴리오 조회 중 오류가 발생했습니다.' });
    }
});

// Admin API - Get all portfolios (for admin)
app.get('/api/admin/portfolio', async (req, res) => {
    if (!req.session.user_id || req.session.role !== 'admin') {
        return res.status(403).json({ error: '관리자만 접근 가능합니다.' });
    }

    try {
        const conn = await pool.getConnection();
        const [portfolios] = await conn.query(
            `SELECT id, title, description, image_path as image, project_url, tags
             FROM portfolio_item 
             ORDER BY id ASC`
        );
        conn.release();

        // Parse tags JSON with error handling
        const result = portfolios.map(p => ({
            ...p,
            tags: p.tags ? (() => {
                try {
                    const parsed = typeof p.tags === 'string' ? JSON.parse(p.tags) : p.tags;
                    return Array.isArray(parsed) ? parsed : [];
                } catch (e) {
                    return [];
                }
            })() : []
        }));

        res.json(result);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '포트폴리오 조회 중 오류가 발생했습니다.' });
    }
});

// Admin API - Add portfolio
app.post('/api/admin/portfolio', async (req, res) => {
    if (!req.session.user_id || req.session.role !== 'admin') {
        return res.status(403).json({ error: '관리자만 접근 가능합니다.' });
    }

    const { title, description, tags, image, projectUrl } = req.body;

    if (!title || !description) {
        return res.status(400).json({ error: '제목과 설명은 필수입니다.' });
    }

    try {
        const conn = await pool.getConnection();
        
        const tagsJson = tags && Array.isArray(tags) ? JSON.stringify(tags) : null;
        
        await conn.query(
            `INSERT INTO portfolio_item (title, description, image_path, project_url, tags) 
             VALUES (?, ?, ?, ?, ?)`,
            [title, description, image || null, projectUrl || '', tagsJson]
        );
        
        conn.release();
        res.json({ success: true, message: '포트폴리오가 추가되었습니다.' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '포트폴리오 추가 중 오류가 발생했습니다.' });
    }
});

// Admin API - Update portfolio
app.put('/api/admin/portfolio/:id', async (req, res) => {
    if (!req.session.user_id || req.session.role !== 'admin') {
        return res.status(403).json({ error: '관리자만 접근 가능합니다.' });
    }

    const { id } = req.params;
    const { title, description, tags, image, projectUrl } = req.body;

    if (!title || !description) {
        return res.status(400).json({ error: '제목과 설명은 필수입니다.' });
    }

    try {
        const conn = await pool.getConnection();
        
        const tagsJson = tags && Array.isArray(tags) ? JSON.stringify(tags) : null;
        
        await conn.query(
            `UPDATE portfolio_item 
             SET title = ?, description = ?, image_path = ?, tags = ?, project_url = ?
             WHERE id = ?`,
            [title, description, image || null, tagsJson, projectUrl || '', id]
        );
        
        conn.release();
        res.json({ success: true, message: '포트폴리오가 수정되었습니다.' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '포트폴리오 수정 중 오류가 발생했습니다.' });
    }
});

// Admin API - Delete portfolio
app.delete('/api/admin/portfolio/:id', async (req, res) => {
    if (!req.session.user_id || req.session.role !== 'admin') {
        return res.status(403).json({ error: '관리자만 접근 가능합니다.' });
    }

    const { id } = req.params;

    try {
        const conn = await pool.getConnection();
        await conn.query('DELETE FROM portfolio_item WHERE id = ?', [id]);
        conn.release();

        res.json({ success: true, message: '포트폴리오가 삭제되었습니다.' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '포트폴리오 삭제 중 오류가 발생했습니다.' });
    }
});
