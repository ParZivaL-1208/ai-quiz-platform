package com.quizzApp.quizzapp.link;

import com.quizzApp.quizzapp.auth.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "teacher_id"}))
public class StudentTeacherLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(optional = false) @JoinColumn(name = "teacher_id")
    private User teacher;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime createdAt;

    public enum Status { PENDING, APPROVED, DECLINED }
}
