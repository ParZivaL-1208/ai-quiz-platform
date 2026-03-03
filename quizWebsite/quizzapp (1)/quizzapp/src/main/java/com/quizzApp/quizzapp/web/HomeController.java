package com.quizzApp.quizzapp.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;

@Controller
public class HomeController {


    @GetMapping({"/", "/home"})
    public String home() {
        return "index";
    }

    @GetMapping("/post-login")
    public String postLogin(Authentication auth) {
        if (auth == null) return "redirect:/";
        var roles = auth.getAuthorities();
        if (roles.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))   return "redirect:/admin";
        if (roles.stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) return "redirect:/teacher";
        return "redirect:/student";
    }
}
