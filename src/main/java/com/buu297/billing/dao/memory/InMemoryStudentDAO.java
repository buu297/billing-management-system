package com.buu297.billing.dao.memory;

import com.buu297.billing.dao.StudentDAO;
import com.buu297.billing.model.Student;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InMemoryStudentDAO implements StudentDAO {
    private final InMemoryDatabase db = InMemoryDatabase.getInstance();

    @Override
    public Optional<Student> findById(String studentId) {
        return Optional.ofNullable(db.students.get(studentId));
    }

    @Override
    public List<Student> findAll() {
        return new ArrayList<>(db.students.values());
    }

    @Override
    public List<Student> search(String query) {
        if (query == null || query.trim().isEmpty()) return findAll();
        String q = query.toLowerCase().trim();
        return db.students.values().stream()
                .filter(s -> s.getStudentId().toLowerCase().contains(q)
                        || s.getFullName().toLowerCase().contains(q)
                        || s.getEmail().toLowerCase().contains(q)
                        || s.getDepartment().toLowerCase().contains(q)
                        || s.getMajor().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    @Override
    public void save(Student student) {
        db.students.put(student.getStudentId(), student);
    }

    @Override
    public void update(Student student) {
        db.students.put(student.getStudentId(), student);
    }

    @Override
    public int countActiveStudents() {
        return (int) db.students.values().stream().filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus())).count();
    }
}