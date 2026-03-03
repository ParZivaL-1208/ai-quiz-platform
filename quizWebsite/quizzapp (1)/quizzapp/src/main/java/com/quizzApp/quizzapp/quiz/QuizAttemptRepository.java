// QuizAttemptRepository.java
package com.quizzApp.quizzapp.quiz;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByStudentUsername(String username);
    List<QuizAttempt> findByQuizCreatedByUsername(String teacherUsername);
    List<QuizAttempt> findByStudentUsernameAndQuizCreatedByUsername(String studentUsername, String teacherUsername);
    boolean existsByStudentUsernameAndQuizId(String username, Long quizId);
}
