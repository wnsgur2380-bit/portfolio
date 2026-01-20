# models.py

from sqlmodel import SQLModel, Field
from typing import Optional
from datetime import datetime

class AnalysisRequest(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True) 
    
    title: str = Field(default="제목 없음")
    author: str = Field(default="작성자 없음", index=True)
    content: Optional[str] = Field(default=None)
    
    email: str = Field(index=True)
    password_hash: str 
    
    # 영상이 필요한 주소 (필수)
    target_address: str 
    
    # 🚨 [업데이트] 다중 파일을 지원하기 위해 JSON 문자열(List) 형태로 저장합니다.
    # 예: '["file1.mp4", "file2.mp4"]'
    original_video_filename: Optional[str] = Field(default="[]") 
    original_video_path: Optional[str] = Field(default="[]")
    
    # 분석 결과 영상 경로 (JSON List)
    analyzed_video_path: Optional[str] = Field(default="[]") 
    
    status: str = Field(default="민원 처리 대기중") 
    
    created_at: datetime = Field(default_factory=datetime.utcnow)