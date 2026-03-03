package com.quizzApp.quizzapp.quiz;

import com.quizzApp.quizzapp.auth.User;
import com.quizzApp.quizzapp.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RetakeRequestService {

    private final RetakeRequestRepository requestRepo;
    private final QuizRepository quizRepo;
    private final QuizAttemptRepository attemptRepo;
    private final UserRepository userRepo;

    @Transactional
    public void requestRetake(String studentUsername, Long quizId) {
        if (!attemptRepo.existsByStudentUsernameAndQuizId(studentUsername, quizId)) {
            throw new IllegalStateException("You must attempt the quiz before requesting a retake.");
        }
        if (requestRepo.existsByStudentUsernameAndQuizIdAndStatus(studentUsername, quizId, RetakeRequest.Status.PENDING)) {
            throw new IllegalStateException("You already have a pending request for this quiz.");
        }

        User student = userRepo.findByUsername(studentUsername).orElseThrow();
        Quiz quiz = quizRepo.findById(quizId).orElseThrow();

        RetakeRequest req = RetakeRequest.builder()
                .student(student)
                .quiz(quiz)
                .status(RetakeRequest.Status.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        requestRepo.save(req);
    }

    public List<RetakeRequest> pendingForTeacher(String teacherUsername) {
        return requestRepo.findByQuizCreatedByUsernameAndStatus(teacherUsername, RetakeRequest.Status.PENDING);
    }

    @Transactional
    public void approve(Long requestId, String teacherUsername) {
        RetakeRequest req = requestRepo.findById(requestId).orElseThrow();
        req.setStatus(RetakeRequest.Status.APPROVED);
        req.setDecidedAt(LocalDateTime.now());
        req.setDecidedBy(userRepo.findByUsername(teacherUsername).orElseThrow());
        requestRepo.save(req);
    }

    @Transactional
    public void decline(Long requestId, String teacherUsername) {
        RetakeRequest req = requestRepo.findById(requestId).orElseThrow();
        req.setStatus(RetakeRequest.Status.DECLINED);
        req.setDecidedAt(LocalDateTime.now());
        req.setDecidedBy(userRepo.findByUsername(teacherUsername).orElseThrow());
        requestRepo.save(req);
    }

    @Transactional
    public boolean consumeApprovedIfAny(String studentUsername, Long quizId) {
        return requestRepo.findFirstByStudentUsernameAndQuizIdAndStatus(
                studentUsername, quizId, RetakeRequest.Status.APPROVED
        ).map(req -> {
            requestRepo.delete(req);
            return true;
        }).orElse(false);
    }
}