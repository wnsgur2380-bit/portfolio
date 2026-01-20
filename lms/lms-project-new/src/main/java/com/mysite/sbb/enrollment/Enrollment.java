package com.mysite.sbb.enrollment;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import com.mysite.sbb.classes.Classes;
import com.mysite.sbb.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "enrollment",
	    uniqueConstraints = {
	        @UniqueConstraint(columnNames = {"classes_id", "uno"})
	    }
	)
public class Enrollment {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="enrollment_id")
    private Long enrollmentId; 

	@ManyToOne
	@JoinColumn(name = "classes_id", nullable = false)
	private Classes classes; // FK: 강의

	@ManyToOne
	@JoinColumn(name = "uno", nullable = false)
	private User user; // FK: 수강 회원


	@Column(name = "progress", nullable = false)
	//@ColumnDefault("0") // length 안붙이는게 나중에 평균 진도율 계산할 때 편함
	private int progress = 0; // 진도율(%) 기본값 0
	
	@Column(name = "is_completed", nullable = false)
	private boolean completed = false; // 수료 여부
	
	@CreationTimestamp //INSERT 되는 순간의 시간값을 자동 기록
	@Column(name = "enrollment_cdate", updatable = false)
	private LocalDateTime enrollmentDate; // 신청일

}