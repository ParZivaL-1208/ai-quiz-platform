package com.quizzApp.quizzapp.web;

import com.quizzApp.quizzapp.link.LinkService;
import com.quizzApp.quizzapp.quiz.Question;
import com.quizzApp.quizzapp.quiz.QuestionRepository;
import com.quizzApp.quizzapp.quiz.Quiz;
import com.quizzApp.quizzapp.quiz.QuizAttemptRepository;
import com.quizzApp.quizzapp.quiz.QuizService;
import com.quizzApp.quizzapp.quiz.RetakeRequestService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final QuizService quizService;
    private final QuizAttemptRepository attemptRepo;
    private final LinkService linkService;
    private final RetakeRequestService retakeRequestService;
    private final QuestionRepository questionRepo;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String UPLOAD_API_URL = "http://127.0.0.1:5000/upload_pdf";

    // Helper method to format chapter names for the Teacher UI
    private String getDisplayChapter(String rawChapter) {
        if (rawChapter == null || rawChapter.trim().isEmpty()) {
            return "Unknown Chapter";
        }
        if (rawChapter.startsWith("Topic not found")) {
            return "Uncategorized";
        }
        return rawChapter;
    }

    @ModelAttribute("pendingJoinCount")
    public int pendingJoinCount(Authentication auth) {
        try {
            if (auth == null) return 0;
            var list = linkService.pendingForTeacher(auth.getName());
            return (list == null) ? 0 : list.size();
        } catch (Exception e) { return 0; }
    }

    @ModelAttribute("pendingRetakeCount")
    public int pendingRetakeCount(Authentication auth) {
        try {
            if (auth == null) return 0;
            var list = retakeRequestService.pendingForTeacher(auth.getName());
            return (list == null) ? 0 : list.size();
        } catch (Exception e) { return 0; }
    }

    @GetMapping
    public String dashboard(Authentication auth, Model model) {
        List<Quiz> displayQuizzes = quizService.quizzesByTeacherUsername(auth.getName())
                .stream()
                .filter(q -> !"Question Bank Repository".equals(q.getTitle()))
                .collect(Collectors.toList());

        List<Question> allQuestions = questionRepo.findByQuizCreatedByUsername(auth.getName());
        Map<String, List<Question>> bankByChapter = allQuestions.stream()
                .collect(Collectors.groupingBy(
                        q -> getDisplayChapter(q.getChapter()),
                        TreeMap::new,
                        Collectors.toList()
                ));

        model.addAttribute("quizzes", displayQuizzes);
        model.addAttribute("form", CreateQuizForm.sample());
        model.addAttribute("bankByChapter", bankByChapter);
        return "teacher";
    }

    @PostMapping("/quiz")
    public String createQuiz(@ModelAttribute CreateQuizForm form, Authentication auth) {
        List<QuizService.QuestionPayload> payload = new ArrayList<>();
        for (CreateQuestion q : form.questions) {
            payload.add(new QuizService.QuestionPayload(
                    q.text, q.optionA, q.optionB, q.optionC, q.optionD, q.correct));
        }
        quizService.createQuiz(form.title, auth.getName(), payload);
        return "redirect:/teacher";
    }

    // --- QUESTION BANK LOGIC ---

    @GetMapping("/bank")
    public String questionBankChapters(Authentication auth, Model model) {
        List<Question> allQuestions = questionRepo.findByQuizCreatedByUsername(auth.getName());

        Set<String> chapters = allQuestions.stream()
                .map(q -> getDisplayChapter(q.getChapter()))
                .collect(Collectors.toCollection(TreeSet::new));

        model.addAttribute("chapters", chapters);
        return "teacher_bank_chapters";
    }

    @GetMapping("/bank/chapter")
    public String questionBankForChapter(@RequestParam("name") String chapterName, Authentication auth, Model model) {
        List<Question> allQuestions = questionRepo.findByQuizCreatedByUsername(auth.getName());

        List<Question> chapterQuestions = allQuestions.stream()
                .filter(q -> getDisplayChapter(q.getChapter()).equals(chapterName))
                .collect(Collectors.toList());

        model.addAttribute("chapterName", chapterName);
        model.addAttribute("questions", chapterQuestions);
        return "teacher_bank_questions";
    }

    @PostMapping("/quiz/from-bank")
    public String createQuizFromBank(@RequestParam String title,
                                     @RequestParam(required = false) List<Long> selectedQuestions,
                                     Authentication auth) {
        if (selectedQuestions != null && !selectedQuestions.isEmpty()) {
            quizService.createQuizFromBank(title, auth.getName(), selectedQuestions);
        }
        return "redirect:/teacher";
    }

    @PostMapping("/bank/add-question")
    public String addQuestionToBank(@RequestParam String chapter,
                                    @RequestParam String text,
                                    @RequestParam String optionA,
                                    @RequestParam String optionB,
                                    @RequestParam String optionC,
                                    @RequestParam String optionD,
                                    @RequestParam String correct,
                                    Authentication auth) {

        quizService.addQuestionToBank(auth.getName(), chapter, text, optionA, optionB, optionC, optionD, correct);
        return "redirect:/teacher/bank/chapter?name=" + URLEncoder.encode(chapter, StandardCharsets.UTF_8);
    }

    @PostMapping("/bank/question/{id}/delete")
    public String deleteBankQuestion(@PathVariable Long id,
                                     Authentication auth,
                                     RedirectAttributes ra) {

        Question q = questionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));

        if (!q.getQuiz().getCreatedBy().getUsername().equals(auth.getName())) {
            throw new SecurityException("You are not authorized to delete this question.");
        }

        String chapter = getDisplayChapter(q.getChapter());

        questionRepo.delete(q);

        ra.addFlashAttribute("message", "Question removed from bank.");

        return "redirect:/teacher/bank/chapter?name=" + URLEncoder.encode(chapter, StandardCharsets.UTF_8);
    }

    // --- REST OF CONTROLLER ---

    @GetMapping("/inbox")
    public String inbox(Authentication auth, Model model) {
        String teacher = (auth != null ? auth.getName() : null);

        java.util.List<?> joins;
        java.util.List<?> retakes;

        try {
            joins = (teacher == null) ? java.util.Collections.emptyList()
                    : linkService.pendingForTeacher(teacher);
        } catch (Exception e) {
            joins = java.util.Collections.emptyList();
        }
        try {
            retakes = (teacher == null) ? java.util.Collections.emptyList()
                    : retakeRequestService.pendingForTeacher(teacher);
        } catch (Exception e) {
            retakes = java.util.Collections.emptyList();
        }

        if (joins == null)   joins = java.util.Collections.emptyList();
        if (retakes == null) retakes = java.util.Collections.emptyList();

        model.addAttribute("joinRequests", joins);
        model.addAttribute("retakeRequests", retakes);
        return "teacher_inbox";
    }

    @GetMapping("/requests")
    public String studentRequests(Authentication auth, Model model) {
        model.addAttribute("requests", linkService.pendingForTeacher(auth.getName()));
        return "teacher_requests";
    }

    @PostMapping("/requests/{id}/approve")
    public String approveStudentRequest(@PathVariable Long id, Authentication auth) {
        linkService.approve(id, auth.getName());
        return "redirect:/teacher/requests";
    }

    @PostMapping("/requests/{id}/decline")
    public String declineStudentRequest(@PathVariable Long id, Authentication auth) {
        linkService.decline(id, auth.getName());
        return "redirect:/teacher/requests";
    }

    @GetMapping("/retakes")
    public String retakes(Authentication auth, Model model) {
        model.addAttribute("requests", retakeRequestService.pendingForTeacher(auth.getName()));
        return "teacher_retake_requests";
    }

    @PostMapping("/retakes/{id}/approve")
    public String approveRetake(@PathVariable Long id, Authentication auth) {
        retakeRequestService.approve(id, auth.getName());
        return "redirect:/teacher/retakes";
    }

    @PostMapping("/retakes/{id}/decline")
    public String declineRetake(@PathVariable Long id, Authentication auth) {
        retakeRequestService.decline(id, auth.getName());
        return "redirect:/teacher/retakes";
    }

    @GetMapping("/students")
    public String students(Authentication auth, Model model) {
        model.addAttribute("students", linkService.approvedStudents(auth.getName()));
        return "teacher_students";
    }

    @GetMapping("/results")
    public String results(Authentication auth, Model model) {
        model.addAttribute("attempts", attemptRepo.findByQuizCreatedByUsername(auth.getName()));
        return "teacher_results";
    }

    @GetMapping("/students/{username}/results")
    public String studentResults(@PathVariable String username,
                                 Authentication auth,
                                 Model model) {
        model.addAttribute("studentUsername", username);
        model.addAttribute("attempts",
                attemptRepo.findByStudentUsernameAndQuizCreatedByUsername(username, auth.getName()));
        return "teacher_student_results";
    }

    @GetMapping("/materials")
    public String showMaterialsPage(Model model) {
        return "teacher_materials";
    }

    @PostMapping("/upload-pdf")
    public String handleFileUpload(@RequestParam("fileUpload") MultipartFile file, RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload.");
            return "redirect:/teacher/materials";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            Resource fileAsResource = file.getResource();
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("fileUpload", fileAsResource);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    UPLOAD_API_URL,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                redirectAttributes.addFlashAttribute("message", "File '" + file.getOriginalFilename() + "' uploaded and processed successfully!");
            }
            else {
                redirectAttributes.addFlashAttribute("error", "Python API reported an error: " + response.getBody());
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not upload file to Python API: " + e.getMessage());
            System.err.println("ERROR calling Python upload API: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/teacher/materials";
    }

    @Data
    public static class CreateQuizForm {
        public String title;
        public List<CreateQuestion> questions = new ArrayList<>();
        static CreateQuizForm sample() {
            CreateQuizForm f = new CreateQuizForm();
            CreateQuestion q = new CreateQuestion();
            q.text = "";
            f.questions.add(q);
            return f;
        }
    }

    @Data
    public static class CreateQuestion {
        public String text;
        public String optionA;
        public String optionB;
        public String optionC;
        public String optionD;
        public String correct;
    }
}