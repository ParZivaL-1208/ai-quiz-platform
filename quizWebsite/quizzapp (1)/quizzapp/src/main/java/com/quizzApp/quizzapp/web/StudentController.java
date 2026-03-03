package com.quizzApp.quizzapp.web;

import com.quizzApp.quizzapp.auth.Role;
import com.quizzApp.quizzapp.auth.User;
import com.quizzApp.quizzapp.auth.UserRepository;
import com.quizzApp.quizzapp.link.LinkService;
import com.quizzApp.quizzapp.quiz.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final QuizService quizService;
    private final QuizRepository quizRepo;
    private final QuizAttemptRepository attemptRepo;
    private final LinkService linkService;
    private final UserRepository userRepo;
    private final RetakeRequestService retakeRequestService;
    private final AnswerRepository answerRepo;

    @GetMapping
    public String dashboard(Authentication auth, Model model) {
        var approvedTeachers = linkService.approvedTeachers(auth.getName());
        model.addAttribute("teachers", approvedTeachers);
        return "student";
    }

    @GetMapping("/teachers")
    public String listTeachers(Authentication auth, Model model) {
        var allTeachers = userRepo.findByRole(Role.TEACHER);
        var myLinks = linkService.linksForStudent(auth.getName());
        Map<Long, com.quizzApp.quizzapp.link.StudentTeacherLink> linkByTeacherId = new HashMap<>();
        for (var l : myLinks) {
            linkByTeacherId.put(l.getTeacher().getId(), l);
        }

        model.addAttribute("allTeachers", allTeachers);
        model.addAttribute("linkByTeacherId", linkByTeacherId);
        return "student_teachers";
    }

    @PostMapping("/teachers/request/{teacherId}")
    public String requestTeacher(@PathVariable Long teacherId, Authentication auth) {
        linkService.requestLink(auth.getName(), teacherId);
        return "redirect:/student/teachers";
    }

    @GetMapping("/quiz/{id}/start")
    public String startQuiz(@PathVariable Long id, Authentication auth, Model model) {
        // 1. Fetch the quiz safely BEFORE the transaction starts
        Quiz quiz = quizRepo.findById(id).orElseThrow();

        try {
            // 2. Attempt to start the quiz
            QuizAttempt attempt = quizService.startAttempt(id, auth.getName());

            List<QuestionView> shuffledQuestions = new ArrayList<>();
            for (Question q : quiz.getQuestions()) {
                List<OptionView> shuffledOptions = new ArrayList<>();
                for (OptionChoice opt : q.getOptions()) {
                    shuffledOptions.add(new OptionView(opt.getId(), opt.getText()));
                }
                Collections.shuffle(shuffledOptions);
                shuffledQuestions.add(new QuestionView(q.getId(), q.getText(), shuffledOptions));
            }
            Collections.shuffle(shuffledQuestions);

            model.addAttribute("quiz", new QuizView(quiz.getId(), quiz.getTitle(), shuffledQuestions));
            model.addAttribute("attemptId", attempt.getId());
            return "take_quiz";

        } catch (Exception ex) {
            // 3. Catch ALL exceptions (including Spring transaction wrappers)
            // and route to the retake request page instead of crashing
            String errorMsg = "You have already taken this quiz.";
            if (ex instanceof IllegalStateException) {
                errorMsg = ex.getMessage();
            }

            model.addAttribute("quiz", quiz);
            model.addAttribute("quizId", id);
            model.addAttribute("message", errorMsg);
            return "attempt_denied";
        }
    }

    @PostMapping("/quiz/{attemptId}/submit")
    public String submit(@PathVariable Long attemptId,
                         @RequestParam Map<String, String> params,
                         Model model) {
        Map<Long, Long> answers = new HashMap<>();
        params.forEach((k, v) -> {
            if (k.startsWith("q_")) {
                answers.put(Long.valueOf(k.substring(2)), Long.valueOf(v));
            }
        });

        QuizAttempt attempt = quizService.submitAttempt(attemptId, answers);
        model.addAttribute("score", attempt.getScore());
        model.addAttribute("feedback", attempt.getFeedback());
        return "quiz_result";
    }

    @GetMapping("/results")
    public String results(Authentication auth, Model model) {
        model.addAttribute("attempts", attemptRepo.findByStudentUsername(auth.getName()));
        return "results_student";
    }

    @GetMapping("/results/{attemptId}")
    public String showAttemptDetails(@PathVariable Long attemptId, Model model, Authentication auth) {
        QuizAttempt attempt = attemptRepo.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));
        if (!attempt.getStudent().getUsername().equals(auth.getName())) {
            return "redirect:/student/results";
        }
        model.addAttribute("attempt", attempt);
        return "attempt_details";
    }

    @GetMapping("/teacher/{teacherId}/quizzes")
    public String showQuizzesByTeacher(@PathVariable Long teacherId, Model model, Authentication auth) {
        User teacher = userRepo.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        List<Quiz> quizzes = quizRepo.findByCreatedByUsername(teacher.getUsername())
                .stream()
                .filter(q -> !"Question Bank Repository".equals(q.getTitle()))
                .collect(Collectors.toList());

        model.addAttribute("teacher", teacher);
        model.addAttribute("quizzes", quizzes);
        return "student_teacher_quizzes";
    }

    @PostMapping("/quiz/{id}/retake-request")
    public String requestRetake(@PathVariable Long id, Authentication auth, Model model) {
        model.addAttribute("quiz", quizRepo.findById(id).orElseThrow());
        model.addAttribute("quizId", id);

        try {
            retakeRequestService.requestRetake(auth.getName(), id);
            model.addAttribute("info", "Retake request sent to your teacher.");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        return "attempt_denied";
    }

    public record OptionView(Long id, String text) {}
    public record QuestionView(Long id, String text, List<OptionView> options) {}
    public record QuizView(Long id, String title, List<QuestionView> questions) {}
}