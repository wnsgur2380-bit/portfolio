const express = require('express');
const path = require('path');
const app = express();

const PORT = 3001;

// 미들웨어
app.use(express.json());
app.use(express.static(path.join(__dirname, '../public')));

// Health check 엔드포인트
app.get('/api/health', (req, res) => {
    res.json({
        status: 'OK',
        message: 'AI Blur Backend 정상 작동',
        timestamp: new Date().toISOString(),
        version: '1.0.0',
        environment: process.env.NODE_ENV || 'development'
    });
});

// API 테스트 엔드포인트
app.get('/api/test', (req, res) => {
    res.json({
        success: true,
        message: 'Backend API 테스트 성공',
        data: {
            service: 'AI Blur',
            features: ['Image Blur', 'Background Processing', 'AI Detection']
        }
    });
});

// 404 핸들러
app.use((req, res) => {
    res.status(404).json({
        error: 'Not Found',
        message: `요청한 경로를 찾을 수 없습니다: ${req.path}`,
        timestamp: new Date().toISOString()
    });
});

// 에러 핸들러
app.use((err, req, res, next) => {
    console.error('Error:', err);
    res.status(500).json({
        error: 'Internal Server Error',
        message: err.message,
        timestamp: new Date().toISOString()
    });
});

app.listen(PORT, () => {
    console.log(`🎨 AI Blur Backend server running on http://localhost:${PORT}`);
    console.log(`📡 API Health: http://localhost:${PORT}/api/health`);
});
