package com.example.legacypoc.service;

import com.example.legacypoc.dao.EmployeeDao;
import com.example.legacypoc.exception.DuplicateEmployeeEmailException;
import com.example.legacypoc.exception.EmployeeNotFoundException;
import com.example.legacypoc.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DefaultEmployeeService implements EmployeeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEmployeeService.class);
    private final EmployeeDao employeeDao;

    public DefaultEmployeeService(EmployeeDao employeeDao) {
        this.employeeDao = employeeDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> getEmployees() {
        return employeeDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getEmployee(Long id) {
        Employee employee = employeeDao.findById(id);
        if (employee == null) {
            LOGGER.warn("Employee not found: {}", id);
            throw new EmployeeNotFoundException(id);
        }
        return employee;
    }

    @Override
    public void createEmployee(Employee employee) {
        normalize(employee);
        if (employeeDao.emailExists(employee.getEmail())) {
            throw new DuplicateEmployeeEmailException(employee.getEmail());
        }
        LOGGER.info("Creating employee with email {}", employee.getEmail());
        employeeDao.insert(employee);
    }

    @Override
    public void updateEmployee(Employee employee) {
        if (employee.getEmployeeId() == null) {
            throw new EmployeeNotFoundException(null);
        }
        normalize(employee);
        if (employeeDao.findById(employee.getEmployeeId()) == null) {
            throw new EmployeeNotFoundException(employee.getEmployeeId());
        }
        if (employeeDao.emailExistsForOtherEmployee(employee.getEmail(), employee.getEmployeeId())) {
            throw new DuplicateEmployeeEmailException(employee.getEmail());
        }
        LOGGER.info("Updating employee {}", employee.getEmployeeId());
        if (employeeDao.update(employee) == 0) {
            throw new EmployeeNotFoundException(employee.getEmployeeId());
        }
    }

    @Override
    public void deleteEmployee(Long id) {
        LOGGER.info("Deleting employee {}", id);
        if (employeeDao.delete(id) == 0) {
            throw new EmployeeNotFoundException(id);
        }
    }

    private void normalize(Employee employee) {
        employee.setFirstName(trim(employee.getFirstName()));
        employee.setLastName(trim(employee.getLastName()));
        employee.setEmail(trim(employee.getEmail()));
        employee.setDepartment(trim(employee.getDepartment()));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
