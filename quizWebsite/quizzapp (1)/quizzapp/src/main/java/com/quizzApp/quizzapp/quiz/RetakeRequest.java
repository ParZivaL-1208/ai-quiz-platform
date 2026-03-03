package com.quizzApp.quizzapp.quiz;
import com.quizzApp.quizzapp.auth.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(
        name = "retake_requests",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "quiz_id", "status"})
)
public class RetakeRequest {

    public enum Status { PENDING, APPROVED, DECLINED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(optional = false) @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime requestedAt;
    private LocalDateTime decidedAt;

    @ManyToOne @JoinColumn(name = "decided_by_id")
    private User decidedBy;
}
