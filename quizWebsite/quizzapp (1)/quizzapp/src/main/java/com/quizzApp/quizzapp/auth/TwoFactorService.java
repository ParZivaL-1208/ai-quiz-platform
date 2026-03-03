package com.quizzApp.quizzapp.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepo;

    // Inject the tester email from properties
    @Value("${app.2fa.tester-email}")
    private String testerEmail;

    @Transactional
    public void generateAndSendOtp(String username) {
        User user = userRepo.findByUsername(username).orElseThrow();

        // 1. Generate 6-digit Code
        String otp = String.format("%06d", new Random().nextInt(999999));

        // 2. Save to DB
        user.setTwoFactorCode(otp);
        user.setTwoFactorExpiry(LocalDateTime.now().plusMinutes(5)); // Valid for 5 mins
        userRepo.save(user);

        // 3. Send Email (Redirecting strictly to TESTER EMAIL)
        sendEmail(testerEmail, "Login OTP for " + username, "Your One-Time Password is: " + otp);
    }

    public boolean verifyOtp(String username, String inputOtp) {
        User user = userRepo.findByUsername(username).orElseThrow();

        if (user.getTwoFactorCode() == null || user.getTwoFactorExpiry() == null) return false;
        if (LocalDateTime.now().isAfter(user.getTwoFactorExpiry())) return false;

        boolean isValid = inputOtp.equals(user.getTwoFactorCode());
        if (isValid) {
            // Clear OTP after success
            user.setTwoFactorCode(null);
            user.setTwoFactorExpiry(null);
            userRepo.save(user);
        }
        return isValid;
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(testerEmail);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            System.out.println("OTP Email sent to " + to);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
}