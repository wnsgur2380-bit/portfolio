const express = require('express');
const session = require('express-session');
const bodyParser = require('body-parser');
const mysql = require('mysql2/promise');
const path = require('path');
const bcrypt = require('bcrypt');

const app = express();
const PORT = 3000;

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
app.use(bodyParser.urlencoded({ extended: true, limit: '50mb' }));
app.use(bodyParser.json({ limit: '50mb' }));

app.use(session({
    secret: 'your-secret-key-change-this',
    resave: false,
    saveUninitialized: true,
    cookie: { maxAge: 24 * 60 * 60 * 1000 }
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
    res.sendFile(path.join(__dirname, 'chat', 'index.html'));
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
            `SELECT post_id as id, title, author, content, views, 
                    created_at, updated_at
             FROM posts
             ORDER BY created_at DESC`
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
        await conn.query('UPDATE posts SET views = views + 1 WHERE post_id = ?', [id]);

        // Get post
        const [posts] = await conn.query(
            `SELECT * FROM posts
             WHERE post_id = ?`,
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
            `INSERT INTO posts (author, password, title, content, created_at, updated_at)
             VALUES (?, ?, ?, ?, NOW(), NOW())`,
            [req.session.full_name, '', title, content]
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
            'SELECT author FROM posts WHERE post_id = ?',
            [id]
        );

        if (posts.length === 0) {
            conn.release();
            return res.status(404).json({ error: '포스트를 찾을 수 없습니다.' });
        }

        if (posts[0].author !== req.session.full_name) {
            conn.release();
            return res.status(403).json({ error: '수정 권한이 없습니다.' });
        }

        await conn.query(
            'UPDATE posts SET title = ?, content = ?, updated_at = NOW() WHERE post_id = ?',
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
            'SELECT author FROM posts WHERE post_id = ?',
            [id]
        );

        if (posts.length === 0) {
            conn.release();
            return res.status(404).json({ error: '포스트를 찾을 수 없습니다.' });
        }

        if (posts[0].author !== req.session.full_name) {
            conn.release();
            return res.status(403).json({ error: '삭제 권한이 없습니다.' });
        }

        await conn.query('DELETE FROM posts WHERE post_id = ?', [id]);
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
            `SELECT c.comment_id, c.post_id, c.user_id, u.username, u.full_name, 
                    c.content, c.created_at, c.updated_at
             FROM comments c
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
        const [posts] = await conn.query('SELECT post_id FROM posts WHERE post_id = ?', [id]);
        if (posts.length === 0) {
            conn.release();
            return res.status(404).json({ error: '포스트를 찾을 수 없습니다.' });
        }

        await conn.query(
            'INSERT INTO comments (post_id, user_id, content, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())',
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
            'SELECT user_id FROM comments WHERE comment_id = ?',
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
            'UPDATE comments SET content = ?, updated_at = NOW() WHERE comment_id = ?',
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
            'SELECT user_id FROM comments WHERE comment_id = ?',
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

        await conn.query('DELETE FROM comments WHERE comment_id = ?', [id]);
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
            `SELECT p.post_id, p.title, u.username, u.full_name, 
                    p.views, p.created_at, p.updated_at
             FROM posts p
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
        await conn.query('DELETE FROM posts WHERE post_id = ?', [id]);
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
            `SELECT c.comment_id, c.post_id, p.title as post_title,
                    u.username, u.full_name, c.content, c.created_at
             FROM comments c
             LEFT JOIN posts p ON c.post_id = p.post_id
             LEFT JOIN user u ON c.user_id = u.id
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
        await conn.query('DELETE FROM comments WHERE comment_id = ?', [id]);
        conn.release();

        res.json({ success: true, message: '댓글이 삭제되었습니다.' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '댓글 삭제 중 오류가 발생했습니다.' });
    }
});

app.listen(PORT, () => {
    console.log(`Server running on http://localhost:${PORT}`);
});

// Portfolio API - Get all portfolios (public)
app.get('/api/portfolio', async (req, res) => {
    try {
        const conn = await pool.getConnection();
        const [portfolios] = await conn.query(
            `SELECT portfolio_id, title, description, tags, image
             FROM portfolio 
             ORDER BY portfolio_id ASC`
        );
        conn.release();

        // Parse tags from JSON string
        const formattedPortfolios = portfolios.map(p => ({
            ...p,
            tags: typeof p.tags === 'string' ? JSON.parse(p.tags) : p.tags
        }));

        res.json(formattedPortfolios);
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
            `SELECT portfolio_id, title, description, tags, image
             FROM portfolio 
             ORDER BY portfolio_id ASC`
        );
        conn.release();

        const formattedPortfolios = portfolios.map(p => ({
            ...p,
            tags: typeof p.tags === 'string' ? JSON.parse(p.tags) : p.tags
        }));

        res.json(formattedPortfolios);
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

    const { title, description, tags, image } = req.body;

    if (!title || !description) {
        return res.status(400).json({ error: '제목과 설명은 필수입니다.' });
    }

    try {
        const conn = await pool.getConnection();
        const tagsJson = JSON.stringify(tags || []);
        
        await conn.query(
            `INSERT INTO portfolio (title, description, tags, image) 
             VALUES (?, ?, ?, ?)`,
            [title, description, tagsJson, image || null]
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
    const { title, description, tags, image } = req.body;

    if (!title || !description) {
        return res.status(400).json({ error: '제목과 설명은 필수입니다.' });
    }

    try {
        const conn = await pool.getConnection();
        const tagsJson = JSON.stringify(tags || []);
        
        await conn.query(
            `UPDATE portfolio 
             SET title = ?, description = ?, tags = ?, image = ?, updated_at = NOW()
             WHERE portfolio_id = ?`,
            [title, description, tagsJson, image || null, id]
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
        await conn.query('DELETE FROM portfolio WHERE portfolio_id = ?', [id]);
        conn.release();

        res.json({ success: true, message: '포트폴리오가 삭제되었습니다.' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: '포트폴리오 삭제 중 오류가 발생했습니다.' });
    }
});
