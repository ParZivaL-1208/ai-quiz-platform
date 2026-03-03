package com.quizzApp.quizzapp.quiz;

import com.quizzApp.quizzapp.auth.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuizAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) private Quiz quiz;
    @ManyToOne(optional = false) private User student;

    private LocalDateTime startedAt;
    private Integer score; // percentage

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();
}