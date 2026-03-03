package com.quizzApp.quizzapp.quiz;

import com.quizzApp.quizzapp.auth.User;
import com.quizzApp.quizzapp.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepo;
    private final QuestionRepository questionRepo;
    private final OptionRepository optionRepo;
    private final QuizAttemptRepository attemptRepo;
    private final AnswerRepository answerRepo;
    private final UserRepository userRepo;
    private final RetakeRequestService retakeService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String CLASSIFIER_API_URL = "http://127.0.0.1:5000/find_chapter";

    @Value("${app.quiz.mastery-threshold:70}")
    private int masteryThreshold;

    @Transactional
    public Quiz createQuiz(String title, String teacherUsername, List<QuestionPayload> qps) {
        User teacher = userRepo.findByUsername(teacherUsername).orElseThrow();
        Quiz quiz = quizRepo.save(Quiz.builder().title(title).createdBy(teacher).build());

        for (QuestionPayload q : qps) {
            String chapter = "Unknown";
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                Map<String, String> requestBody = Collections.singletonMap("question", q.text());
                HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
                Map<String, Object> response = restTemplate.postForObject(
                        CLASSIFIER_API_URL,
                        requestEntity,
                        Map.class
                );

                if (response != null && response.containsKey("most_likely_chapter")) {
                    chapter = (String) response.get("most_likely_chapter");
                }

            } catch (Exception e) {
                System.err.println("ERROR: Could not connect to Python classifier API: " + e.getMessage());
            }

            Question question = questionRepo.save(
                    Question.builder()
                            .quiz(quiz)
                            .text(q.text())
                            .chapter(chapter)
                            .build());
            optionRepo.save(OptionChoice.builder()
                    .question(question).text(q.a()).correct("A".equals(q.correct())).build());
            optionRepo.save(OptionChoice.builder()
                    .question(question).text(q.b()).correct("B".equals(q.correct())).build());
            optionRepo.save(OptionChoice.builder()
                    .question(question).text(q.c()).correct("C".equals(q.correct())).build());
            optionRepo.save(OptionChoice.builder()
                    .question(question).text(q.d()).correct("D".equals(q.correct())).build());
        }
        return quiz;
    }

    @Transactional
    public Quiz createQuizFromBank(String title, String teacherUsername, List<Long> questionIds) {
        User teacher = userRepo.findByUsername(teacherUsername).orElseThrow();
        Quiz quiz = quizRepo.save(Quiz.builder().title(title).createdBy(teacher).build());

        for (Long qId : questionIds) {
            Question originalQ = questionRepo.findById(qId).orElseThrow();

            Question newQ = questionRepo.save(Question.builder()
                    .quiz(quiz)
                    .text(originalQ.getText())
                    .chapter(originalQ.getChapter())
                    .build());

            for (OptionChoice opt : originalQ.getOptions()) {
                optionRepo.save(OptionChoice.builder()
                        .question(newQ)
                        .text(opt.getText())
                        .correct(opt.isCorrect())
                        .build());
            }
        }
        return quiz;
    }

    @Transactional
    public QuizAttempt startAttempt(Long quizId, String username) {
        boolean alreadyAttempted = attemptRepo.existsByStudentUsernameAndQuizId(username, quizId);

        if (alreadyAttempted) {
            if (!retakeService.consumeApprovedIfAny(username, quizId)) {
                throw new IllegalStateException("You have already taken this quiz.");
            }
        }

        User student = userRepo.findByUsername(username).orElseThrow();
        Quiz quiz = quizRepo.findById(quizId).orElseThrow();

        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .student(student)
                .startedAt(LocalDateTime.now())
                .build();

        return attemptRepo.save(attempt);
    }

    public List<Quiz> quizzesByTeacherUsername(String username) {
        return quizRepo.findByCreatedByUsername(username);
    }

    @Transactional
    public void addQuestionToBank(String teacherUsername, String chapter, String text, String optA, String optB, String optC, String optD, String correct) {
        User teacher = userRepo.findByUsername(teacherUsername).orElseThrow();

        Quiz bankQuiz = quizRepo.findByCreatedByUsername(teacherUsername).stream()
                .filter(q -> "Question Bank Repository".equals(q.getTitle()))
                .findFirst()
                .orElseGet(() -> quizRepo.save(Quiz.builder().title("Question Bank Repository").createdBy(teacher).build()));

        Question question = questionRepo.save(Question.builder()
                .quiz(bankQuiz)
                .text(text)
                .chapter(chapter)
                .build());

        optionRepo.save(OptionChoice.builder().question(question).text(optA).correct("A".equals(correct)).build());
        optionRepo.save(OptionChoice.builder().question(question).text(optB).correct("B".equals(correct)).build());
        optionRepo.save(OptionChoice.builder().question(question).text(optC).correct("C".equals(correct)).build());
        optionRepo.save(OptionChoice.builder().question(question).text(optD).correct("D".equals(correct)).build());
    }

    @Transactional
    public QuizAttempt submitAttempt(Long attemptId, Map<Long, Long> answersByQuestionId) {
        QuizAttempt attempt = attemptRepo.findById(attemptId).orElseThrow();

        int correctCount = 0;
        int total = answersByQuestionId.size();

        Map<String, Integer> chapterTotal = new HashMap<>();
        Map<String, Integer> chapterCorrect = new HashMap<>();

        for (Map.Entry<Long, Long> e : answersByQuestionId.entrySet()) {
            Question q = questionRepo.findById(e.getKey()).orElseThrow();
            OptionChoice selected = optionRepo.findById(e.getValue()).orElseThrow();
            boolean correct = selected.isCorrect();
            if (correct) correctCount++;

            String ch = (q.getChapter() != null) ? q.getChapter() : "Unknown";
            chapterTotal.put(ch, chapterTotal.getOrDefault(ch, 0) + 1);
            if (correct) {
                chapterCorrect.put(ch, chapterCorrect.getOrDefault(ch, 0) + 1);
            }

            answerRepo.save(Answer.builder()
                    .attempt(attempt)
                    .question(q)
                    .selected(selected)
                    .correct(correct)
                    .build());
        }

        int score = (int) Math.round((correctCount * 100.0) / Math.max(1, total));
        attempt.setScore(score);

        StringBuilder feedback = new StringBuilder();
        if (score >= 90) {
            feedback.append("Excellent performance. You demonstrate a strong grasp of the material.");
        } else if (score >= masteryThreshold) {
            feedback.append("Passing performance. Review the identified weak areas to achieve mastery.");
        } else {
            feedback.append("Performance below passing threshold. Detailed review of the material is required.");
        }

        List<String> weakChapters = new ArrayList<>();
        chapterTotal.forEach((ch, count) -> {
            double pct = (chapterCorrect.getOrDefault(ch, 0) * 100.0) / count;
            if (pct < masteryThreshold) {
                weakChapters.add(ch);
            }
        });

        if (!weakChapters.isEmpty()) {
            feedback.append("\n\nSpecific areas for improvement: ").append(String.join(", ", weakChapters));
        }

        attempt.setFeedback(feedback.toString());
        return attemptRepo.save(attempt);
    }

    public record QuestionPayload(String text, String a, String b, String c, String d, String correct) {}
}