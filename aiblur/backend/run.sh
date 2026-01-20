#!/bin/bash

# AI Blur Backend 실행 스크립트
cd /opt/aiblur/backend

# 가상환경 활성화
source venv/bin/activate

# FastAPI 서버 실행 (포트 3001, 모든 호스트에서 접근 가능)
uvicorn main:app --host 0.0.0.0 --port 3001 --reload
