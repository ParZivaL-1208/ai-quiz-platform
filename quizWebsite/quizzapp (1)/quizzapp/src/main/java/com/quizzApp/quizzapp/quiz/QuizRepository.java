package com.quizzApp.quizzapp.quiz;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByCreatedByUsername(String username);
}