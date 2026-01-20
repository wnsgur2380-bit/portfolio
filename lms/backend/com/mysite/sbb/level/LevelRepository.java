package com.mysite.sbb.level;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LevelRepository extends JpaRepository<Level, Long> {
	Optional<Level> findByLevelName(String levelName);
}
