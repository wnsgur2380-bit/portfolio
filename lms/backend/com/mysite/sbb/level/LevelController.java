package com.mysite.sbb.level;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/level")
public class LevelController {
	
	private final LevelService levels;
	
	// 전체 레벨 조회
	@GetMapping("/list")
	public ResponseEntity<List<Level>> getAllLevels(){
		return ResponseEntity.ok(levels.getAllLevel());
	}
	
	// 레벨 상세 조회
    @GetMapping("/{levelId}")
    public ResponseEntity<Level> getLevel(@PathVariable Long levelId) {
        Level level = levels.getLevel(levelId);
        return ResponseEntity.ok(level);
    }
    
	// 레벨 생성
	@PostMapping("/create")
	public ResponseEntity<Level> createLevel(@RequestBody Level level) {
		Level createdLevel = levels.createLevel(level);
		return ResponseEntity.ok(createdLevel);
	}
	
	// 레벨 수정
    @PutMapping("/{levelId}")
    public ResponseEntity<Level> updateLevel(
            @PathVariable Long levelId,
            @RequestBody Level updatedLevel) {
        Level level = levels.updateLevel(levelId, updatedLevel);
        return ResponseEntity.ok(level);
    }

    //레벨삭제
	@DeleteMapping("/{levelId}")
	public ResponseEntity<String> deleteLevel(@PathVariable Long levelId) {
		levels.deleteLevel(levelId);
		return ResponseEntity.ok("레벨 삭제 완료되었습니다.");
	}
}
