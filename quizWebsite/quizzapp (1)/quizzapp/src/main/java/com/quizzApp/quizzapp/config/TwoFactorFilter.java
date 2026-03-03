package com.quizzApp.quizzapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TwoFactorFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI();

        // Pass through static resources, public pages, or the 2FA page itself
        if (path.startsWith("/css") || path.startsWith("/js") || path.equals("/verify-2fa") || path.equals("/logout")) {
            filterChain.doFilter(request, response);
            return;
        }

        // If User is Logged In
        if (auth != null && auth.isAuthenticated() && !path.equals("/")) {
            HttpSession session = request.getSession(false);

            // Check if they have the "2FA_VERIFIED" attribute
            boolean is2faVerified = (session != null && Boolean.TRUE.equals(session.getAttribute("2FA_VERIFIED")));

            if (!is2faVerified) {
                // If not verified, redirect to OTP page
                response.sendRedirect("/verify-2fa");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}