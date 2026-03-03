package com.quizzApp.quizzapp.quiz;

import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Answer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) private QuizAttempt attempt;
    @ManyToOne(optional = false) private Question question;
    @ManyToOne(optional = false) private OptionChoice selected;

    private boolean correct;
}
