package com.quizzApp.quizzapp.web;

import com.quizzApp.quizzapp.auth.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService users;

    @GetMapping("/password")
    public String passwordForm(Model model) {
        model.addAttribute("form", new ChangePasswordForm());
        model.addAttribute("message", null);
        model.addAttribute("error", null);
        return "change_password";
    }

    @PostMapping("/password")
    public String changePassword(@ModelAttribute("form") ChangePasswordForm form,
                                 Authentication auth,
                                 Model model) {
        try {
            if (!form.getNewPassword().equals(form.getConfirmNewPassword())) {
                throw new IllegalArgumentException("New passwords do not match");
            }
            users.changePassword(auth.getName(), form.getOldPassword(), form.getNewPassword());
            var roles = auth.getAuthorities();
            if (roles.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return "redirect:/admin";
            }
            if (roles.stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) {
                return "redirect:/teacher";
            }
            return "redirect:/student";

        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("message", null);
            return "change_password";
        }
    }

    @Data
    public static class ChangePasswordForm {
        public String oldPassword;
        public String newPassword;
        public String confirmNewPassword;
    }
}
