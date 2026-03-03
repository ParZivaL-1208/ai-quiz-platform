package com.quizzApp.quizzapp.link;

import com.quizzApp.quizzapp.auth.User;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentTeacherLinkRepository extends JpaRepository<StudentTeacherLink, Long> {
    boolean existsByStudentAndTeacher(User student, User teacher);
    Optional<StudentTeacherLink> findByStudentAndTeacher(User student, User teacher);
    List<StudentTeacherLink> findByStudentUsername(String username);
    List<StudentTeacherLink> findByTeacherUsernameAndStatus(String username, StudentTeacherLink.Status status);
}
