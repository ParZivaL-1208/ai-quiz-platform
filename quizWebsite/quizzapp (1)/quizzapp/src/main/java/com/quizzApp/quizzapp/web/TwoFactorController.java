package com.quizzApp.quizzapp.web;

import com.quizzApp.quizzapp.auth.TwoFactorService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class TwoFactorController {

    private final TwoFactorService twoFactorService;

    @GetMapping("/verify-2fa")
    public String verifyPage() {
        return "verify_2fa";
    }

    @PostMapping("/verify-2fa")
    public String verifyLogic(@RequestParam String code,
                              Authentication auth,
                              HttpSession session,
                              Model model) {
        if (twoFactorService.verifyOtp(auth.getName(), code)) {
            // MARK SESSION AS VERIFIED
            session.setAttribute("2FA_VERIFIED", true);
            return "redirect:/post-login"; // Send them to the role-router
        } else {
            model.addAttribute("error", "Invalid or expired OTP.");
            return "verify_2fa";
        }
    }
}