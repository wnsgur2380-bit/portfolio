package com.mysite.sbb.payment;

import com.mysite.sbb.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByUserAndLevel_LevelId(User user, Long levelId);
}