package com.mysite.sbb.payment;

import com.mysite.sbb.level.Level;
import com.mysite.sbb.level.LevelRepository;
import com.mysite.sbb.user.User;
import com.mysite.sbb.user.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final LevelRepository levelRepository;
    private final UserService userService;

    @Transactional // [추가] DB 변경이 일어나므로 트랜잭션 처리
    public Payment confirm(String paymentKey, String orderId, int amount, User user, Long levelId) {

        // 1. 결제 정보 저장 (기존 로직)
        Level level = levelRepository.findById(levelId)
                .orElseThrow(() -> new RuntimeException("레벨을 찾을 수 없습니다."));

        Payment payment = new Payment();
        payment.setPaymentKey(paymentKey);
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setStatus("SUCCESS"); // "DONE" or "SUCCESS"
        payment.setPaidAt(LocalDateTime.now());
        payment.setUser(user);
        payment.setLevel(level);

        paymentRepository.save(payment);

        // 2. [핵심 추가] 결제 성공 시 회원 등급 UP (VIP 전환)
        // 여기서 UserService의 upgradeToVip 메서드를 호출하여 실제 권한을 변경
        userService.upgradeToVip(user);

        return payment;
    }

    public boolean hasPaid(User user, Long levelId) {
        return paymentRepository.existsByUserAndLevel_LevelId(user, levelId);
    }
}