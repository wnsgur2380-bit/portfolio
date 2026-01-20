package com.mysite.sbb.payment;

import com.mysite.sbb.user.User;
import com.mysite.sbb.level.Level;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // [수정] 필수 정보이므로 null 불가 처리
    @Column(nullable = false)
    private String paymentKey;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private String status; // SUCCESS / FAIL

    @Column(nullable = false)
    private LocalDateTime paidAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "level_id", nullable = false)
    private Level level;
}