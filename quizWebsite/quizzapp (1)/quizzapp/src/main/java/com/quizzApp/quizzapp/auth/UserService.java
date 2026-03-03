package com.quizzApp.quizzapp.auth;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public List<User> all() { return repo.findAll(); }

    public User create(String username, String email, String rawPassword, Role role) {
        User u = User.builder()
                .username(username)
                .email(email)
                .password(encoder.encode(rawPassword))
                .role(role)
                .build();
        return repo.save(u);
    }

    public void delete(Long id) { repo.deleteById(id); }

    public void changePassword(String username, String oldRaw, String newRaw) {
        User u = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!encoder.matches(oldRaw, u.getPassword())) {
            throw new IllegalArgumentException("Old password does not match");
        }
        u.setPassword(encoder.encode(newRaw));
        repo.save(u);
    }
}
