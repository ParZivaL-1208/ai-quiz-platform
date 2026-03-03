package com.quizzApp.quizzapp.config;

import com.quizzApp.quizzapp.auth.TwoFactorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, TwoFactorService twoFactorService) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/verify-2fa", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/account/**").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/teacher/**").hasRole("TEACHER")
                        .requestMatchers("/student/**").hasRole("STUDENT")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        /* --- 2FA DISABLED FOR TESTING ---
                        .successHandler((request, response, authentication) -> {
                            twoFactorService.generateAndSendOtp(authentication.getName());
                            response.sendRedirect("/verify-2fa");
                        })
                        */
                        .defaultSuccessUrl("/post-login", true) // Direct redirect, bypassing 2FA
                )
                .logout(logout -> logout.logoutSuccessUrl("/"));

                /* --- 2FA FILTER DISABLED FOR TESTING ---
                .addFilterBefore(new TwoFactorFilter(), UsernamePasswordAuthenticationFilter.class);
                */

        return http.build();
    }
}