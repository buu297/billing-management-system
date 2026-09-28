package com.buu297.billing.dao;

import com.buu297.billing.model.Student;
import java.util.List;
import java.util.Optional;

public interface StudentDAO {
    Optional<Student> findById(String studentId);
    List<Student> findAll();
    List<Student> search(String query);
    void save(Student student);
    void update(Student student);
    int countActiveStudents();
}
