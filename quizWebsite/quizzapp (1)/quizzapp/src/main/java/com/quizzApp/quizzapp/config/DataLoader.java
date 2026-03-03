package com.quizzApp.quizzapp.config;

import com.quizzApp.quizzapp.auth.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (repo.findByUsername("admin").isEmpty()) {
            repo.save(User.builder()
                    .username("admin").email("admin@example.com")
                    .password(encoder.encode("admin123"))
                    .role(Role.ADMIN).build());
        }
        if (repo.findByUsername("teacher1").isEmpty()) {
            repo.save(User.builder()
                    .username("teacher1").email("t1@example.com")
                    .password(encoder.encode("teacher123"))
                    .role(Role.TEACHER).build());
        }
        if (repo.findByUsername("student1").isEmpty()) {
            repo.save(User.builder()
                    .username("student1").email("s1@example.com")
                    .password(encoder.encode("student123"))
                    .role(Role.STUDENT).build());
        }
    }
}
