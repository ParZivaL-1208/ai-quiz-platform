package com.quizzApp.quizzapp.quiz;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetakeRequestRepository extends JpaRepository<RetakeRequest, Long> {
    boolean existsByStudentUsernameAndQuizIdAndStatus(String username, Long quizId, RetakeRequest.Status status);
    List<RetakeRequest> findByQuizCreatedByUsernameAndStatus(String teacherUsername, RetakeRequest.Status status);
    Optional<RetakeRequest> findFirstByStudentUsernameAndQuizIdAndStatus(String username, Long quizId, RetakeRequest.Status status);
}
