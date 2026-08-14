package com.example.legacypoc.dao;

import com.example.legacypoc.model.Employee;

import java.util.List;

public interface EmployeeDao {
    List<Employee> findAll();
    Employee findById(Long employeeId);
    int insert(Employee employee);
    int update(Employee employee);
    int delete(Long employeeId);
    boolean emailExists(String email);
    boolean emailExistsForOtherEmployee(String email, Long employeeId);
}
