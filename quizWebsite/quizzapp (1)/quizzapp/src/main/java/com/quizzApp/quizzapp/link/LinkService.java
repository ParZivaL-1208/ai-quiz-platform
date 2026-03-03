package com.quizzApp.quizzapp.link;

import com.quizzApp.quizzapp.auth.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final StudentTeacherLinkRepository linkRepo;
    private final UserRepository userRepo;

    public List<StudentTeacherLink> linksForStudent(String studentUsername) {
        return linkRepo.findByStudentUsername(studentUsername);
    }

    public List<StudentTeacherLink> pendingForTeacher(String teacherUsername) {
        return linkRepo.findByTeacherUsernameAndStatus(
                teacherUsername, StudentTeacherLink.Status.PENDING);
    }

    @Transactional
    public void requestLink(String studentUsername, Long teacherId) {
        User student = userRepo.findByUsername(studentUsername).orElseThrow();
        User teacher = userRepo.findById(teacherId).orElseThrow();
        if (teacher.getRole() != Role.TEACHER) {
            throw new IllegalArgumentException("Not a teacher");
        }
        if (linkRepo.existsByStudentAndTeacher(student, teacher)) return;

        linkRepo.save(StudentTeacherLink.builder()
                .student(student)
                .teacher(teacher)
                .status(StudentTeacherLink.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void approve(Long linkId, String teacherUsername) {
        var link = linkRepo.findById(linkId).orElseThrow();
        if (!link.getTeacher().getUsername().equals(teacherUsername)) {
            throw new IllegalArgumentException("Not yours");
        }
        link.setStatus(StudentTeacherLink.Status.APPROVED);
        linkRepo.save(link);
    }

    @Transactional
    public void decline(Long linkId, String teacherUsername) {
        var link = linkRepo.findById(linkId).orElseThrow();
        if (!link.getTeacher().getUsername().equals(teacherUsername)) {
            throw new IllegalArgumentException("Not yours");
        }
        link.setStatus(StudentTeacherLink.Status.DECLINED);
        linkRepo.save(link);
    }

    public List<User> approvedTeachers(String studentUsername) {
        var links = linksForStudent(studentUsername);
        List<User> teachers = new ArrayList<>();
        for (var l : links) {
            if (l.getStatus() == StudentTeacherLink.Status.APPROVED) {
                teachers.add(l.getTeacher());
            }
        }
        return teachers;
    }
    public List<User> approvedStudents(String teacherUsername) {
        var links = linkRepo.findByTeacherUsernameAndStatus(
                teacherUsername, StudentTeacherLink.Status.APPROVED);
        List<User> students = new ArrayList<>();
        for (var l : links) students.add(l.getStudent());
        return students;
    }

}
