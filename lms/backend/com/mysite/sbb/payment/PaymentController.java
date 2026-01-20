package com.mysite.sbb.payment;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mysite.sbb.user.User;
import com.mysite.sbb.user.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    // 기존 API (필요시 사용)
    @PostMapping("/confirm")
    @ResponseBody
    public ResponseEntity<?> confirm(@RequestBody PaymentConfirmRequest req) {
        User user = userService.getCurrentUser();
        Payment payment = paymentService.confirm(
                req.getPaymentKey(),
                req.getOrderId(),
                req.getAmount(),
                user,
                req.getLevelId()
        );
        return ResponseEntity.ok(payment);
    }

    // 1. 결제 페이지 진입 (VIP 회원은 접근 차단)
    @GetMapping("/membership")
    public String membershipPage(Model model, Principal principal, RedirectAttributes redirectAttributes) {
        if(principal != null) {
            User user = userService.getUser(principal.getName());
            
            // 이미 결제한 사람(VIP)은 페이지 접속 자체를 차단
            if (user.isPaid()) {
                redirectAttributes.addFlashAttribute("errorMsg", "이미 VIP 멤버십 회원입니다.");
                return "redirect:/mainpage";
            }
            // ----------------------------------------------------------
            
            model.addAttribute("user", user);
        }
        return "payment_membership";
    }

    // 2. 결제 성공 처리 (토스 인증 후 복귀)
    @GetMapping("/success")
    public String paymentSuccess(
            //  500 오류 방지를 위해 ("이름") 명시
            @RequestParam("paymentKey") String paymentKey,
            @RequestParam("orderId") String orderId,
            @RequestParam("amount") int amount,
            // ---------------------------------------------
            Principal principal,
            RedirectAttributes redirectAttributes) {

        User user = userService.getUser(principal.getName());
        
        // 중복 결제 요청 방지 (이중 안전장치)
        if (user.isPaid()) {
            redirectAttributes.addFlashAttribute("errorMsg", "이미 VIP 등급이 적용되어 있습니다.");
            return "redirect:/mainpage";
        }
        // ------------------------------------------------------

        Long vipLevelId = 3L; // VIP용 레벨 ID (필요에 따라 수정)

        try {
            paymentService.confirm(paymentKey, orderId, amount, user, vipLevelId);
            redirectAttributes.addFlashAttribute("msg", "결제가 정상적으로 완료되었습니다! (VIP 적용)");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "결제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/mainpage";
    }

    // 3. 결제 실패 처리
    @GetMapping("/fail")
    public String paymentFail(
            //  500 오류 방지를 위해 ("이름") 명시 
            @RequestParam("message") String message,
            @RequestParam("code") String code,
            // -------------------------------------
            RedirectAttributes redirectAttributes) {
        
        redirectAttributes.addFlashAttribute("errorMsg", "결제 실패: " + message);
        return "redirect:/api/payments/membership";
    }

    // 4. 보여주기식 가짜 결제 처리 (테스트용)
    @PostMapping("/process")
    public String processPayment(Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.getUser(principal.getName());
        
        if (user.isPaid()) {
            redirectAttributes.addFlashAttribute("errorMsg", "이미 VIP 회원입니다.");
            return "redirect:/mainpage";
        }
        
        userService.upgradeToVip(user);
        redirectAttributes.addFlashAttribute("msg", "멤버십 가입이 완료되었습니다! (VIP 등급 적용)");
        
        return "redirect:/mainpage";
    }
}