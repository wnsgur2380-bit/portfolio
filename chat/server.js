const express = require('express');
const ExpressWs = require('express-ws');
const path = require('path');

const app = express();
ExpressWs(app);

// 정적 파일 제공
app.use(express.static(__dirname));

// 접속한 클라이언트 저장
const clients = new Map();

// WebSocket 라우트
app.ws('/ws', (ws, req) => {
    const clientId = Date.now().toString();
    const clientInfo = {
        ws: ws,
        nickname: null,
        id: clientId
    };

    // 새 클라이언트 정보 저장
    clients.set(clientId, clientInfo);
    console.log(`[클라이언트 연결] ${clientId}`);

    // 메시지 수신
    ws.on('message', (msg) => {
        try {
            const data = JSON.parse(msg);

            // 닉네임 설정
            if (data.type === 'nickname') {
                clientInfo.nickname = data.nickname;
                console.log(`[닉네임 설정] ${clientId}: ${data.nickname}`);

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
                console.log(`[메시지] ${data.nickname}: ${data.message}`);

                // 모든 클라이언트에게 메시지 전송
                clients.forEach((client, id) => {
                    if (client.ws && client.ws.readyState === 1) { // OPEN 상태
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

    // 연결 종료
    ws.on('close', () => {
        const nickname = clientInfo.nickname;
        clients.delete(clientId);
        console.log(`[클라이언트 종료] ${clientId}: ${nickname}`);

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
        console.error(`[WebSocket 에러] ${clientId}:`, error);
    });
});

// 모든 클라이언트에게 사용자 수 전송
function broadcastUserCount() {
    const count = clients.size;
    clients.forEach((client) => {
        if (client.ws && client.ws.readyState === 1) { // OPEN 상태
            client.ws.send(JSON.stringify({
                type: 'userCount',
                count: count
            }));
        }
    });
}

// 특정 클라이언트를 제외한 모든 클라이언트에게 메시지 전송
function broadcast(message, excludeId = null) {
    clients.forEach((client, id) => {
        if (client.ws && client.ws.readyState === 1 && id !== excludeId) { // OPEN 상태
            client.ws.send(JSON.stringify(message));
        }
    });
}

const PORT = 3000;
const HOST = '0.0.0.0';

app.listen(PORT, HOST, () => {
    console.log(`[웹 채팅 서버 시작] http://localhost:${PORT}/chat`);
    console.log(`[접속 URL] http://localhost:${PORT}/chat`);
});
