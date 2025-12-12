#!/bin/bash

# 자동 서버 재시작 스크립트
# 파일 변경 감지 시 자동으로 서버 재시작

cd /opt/www

echo "🔍 파일 변경 감지 시작..."

# inotify-tools 설치 확인
if ! command -v inotifywait &> /dev/null; then
    echo "inotify-tools 설치 중..."
    sudo apt-get update > /dev/null 2>&1
    sudo apt-get install -y inotify-tools > /dev/null 2>&1
fi

# 초기 서버 시작
restart_server() {
    echo "🔄 서버 재시작 중..."
    pkill -f "node server.js" 2>/dev/null
    sleep 1
    cd /opt/www && node server.js > /tmp/server.log 2>&1 &
    sleep 2
    echo "✅ 서버 시작됨 ($(date '+%H:%M:%S'))"
}

# 초기 서버 시작
restart_server

# public 폴더의 파일 변경 감지
inotifywait -m -r -e modify,create,delete /opt/www/public --format '%w%f' |
while read file; do
    if [[ $file == *.html ]] || [[ $file == *.css ]] || [[ $file == *.js ]]; then
        echo "📝 파일 변경 감지: $file"
        restart_server
    fi
done
