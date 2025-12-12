const http = require('http');
const fs = require('fs');
const path = require('path');
const WebSocketServer = require('ws').Server;

const PORT = 3000;

// HTTP 서버
const server = http.createServer((req, res) => {
    if (req.url === '/chat' || req.url === '/') {
        fs.readFile(path.join(__dirname, 'index.html'), 'utf8', (err, data) => {
            if (err) {
                res.writeHead(500, { 'Content-Type': 'text/html; charset=utf-8' });
                res.end('파일을 찾을 수 없습니다.');
            } else {
                res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
                res.end(data);
            }
        });
    } else {
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('404 Not Found');
    }
});

// WebSocket 서버
const wss = new WebSocketServer({ server });

const clients = new Map();

wss.on('connection', (ws) => {
    const clientId = Date.now().toString();
    const clientInfo = {
        ws: ws,
        nickname: null,
        id: clientId
    };

    clients.set(clientId, clientInfo);
    console.log(`[클라이언트 연결] ${clientId} (총: ${clients.size}명)`);

    ws.on('message', (msg) => {
        try {
            const data = JSON.parse(msg);

            if (data.type === 'nickname') {
                clientInfo.nickname = data.nickname;
                console.log(`[닉네임 설정] ${clientId}: ${data.nickname}`);

                broadcastUserCount();
                broadcast({
                    type: 'userJoined',
                    nickname: data.nickname
                }, clientId);
            }
            else if (data.type === 'message') {
                console.log(`[메시지] ${data.nickname}: ${data.message}`);

                clients.forEach((client, id) => {
                    if (client.ws.readyState === 1) {
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
            console.error('메시지 처리 오류:', error);
        }
    });

    ws.on('close', () => {
        const nickname = clientInfo.nickname;
        clients.delete(clientId);
        console.log(`[클라이언트 종료] ${clientId}: ${nickname} (총: ${clients.size}명)`);

        if (nickname) {
            broadcast({
                type: 'userLeft',
                nickname: nickname
            });
            broadcastUserCount();
        }
    });

    ws.on('error', (error) => {
        console.error(`[에러] ${clientId}:`, error);
    });
});

function broadcastUserCount() {
    const count = clients.size;
    clients.forEach((client) => {
        if (client.ws.readyState === 1) {
            client.ws.send(JSON.stringify({
                type: 'userCount',
                count: count
            }));
        }
    });
}

function broadcast(message, excludeId = null) {
    clients.forEach((client, id) => {
        if (client.ws.readyState === 1 && id !== excludeId) {
            client.ws.send(JSON.stringify(message));
        }
    });
}

server.listen(PORT, '0.0.0.0', () => {
    console.log(`[웹 채팅 서버 시작] http://localhost:${PORT}/chat`);
});
