package com.quizzApp.quizzapp.web;

import com.quizzApp.quizzapp.auth.Role;
import com.quizzApp.quizzapp.auth.User;
import com.quizzApp.quizzapp.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("users", userRepo.findAll());
        model.addAttribute("roles", Role.values());
        return "admin";
    }

    @PostMapping("/admin/users")
    public String createUser(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam Role role,
                             RedirectAttributes ra) {
        if (userRepo.findByUsername(username).isPresent()) {
            ra.addFlashAttribute("err", "Username already exists.");
            return "redirect:/admin";
        }
        User u = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .build();
        userRepo.save(u);
        ra.addFlashAttribute("ok", "User created.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             Authentication auth,
                             RedirectAttributes ra) {
        try {
            User targetUser = userRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            String currentUsername = auth.getName();

            if (targetUser.getUsername().equals(currentUsername)) {
                ra.addFlashAttribute("err", "Action denied: You cannot delete your own account.");
                return "redirect:/admin";
            }

            if (targetUser.getRole() == Role.ADMIN) {
                List<User> allAdmins = userRepo.findByRole(Role.ADMIN);
                if (allAdmins.size() <= 1) {
                    ra.addFlashAttribute("err", "Action denied: Cannot delete the only remaining Administrator.");
                    return "redirect:/admin";
                }
            }

            userRepo.deleteById(id);
            ra.addFlashAttribute("ok", "User deleted successfully.");

        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("err",
                    "Cannot delete user; they have related data (quizzes/attempts/links).");
        } catch (Exception ex) {
            ra.addFlashAttribute("err", "Error: " + ex.getMessage());
        }
        return "redirect:/admin";
    }
}
