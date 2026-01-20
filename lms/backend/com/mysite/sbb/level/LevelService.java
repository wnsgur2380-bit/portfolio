package com.mysite.sbb.level;

import java.util.List;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class LevelService {
	
	private final LevelRepository levelr;
	
	// 전체 레벨 조회
	public List<Level> getAllLevel(){
		return levelr.findAll();
	}
	
	// 레벨 생성
	public Level createLevel(Level level) {
		return levelr.save(level);
	}
	
	//단일 조회 (필요 시 FK 연결용)
	public Level getLevel(Long levelId) {
	    return levelr.findById(levelId)
	    .orElseThrow(() -> new IllegalArgumentException("해당 레벨을 찾을 수 없습니다. ID: " + levelId));
	}


	// 수정
    public Level updateLevel(Long levelId, Level updatedLevel) {
        Level existing = getLevel(levelId);
        existing.setLevelName(updatedLevel.getLevelName());
        return levelr.save(existing);
    }
    
	// 레벨 삭제
	public void deleteLevel(Long levelId) {
		levelr.deleteById(levelId);
	}
}
