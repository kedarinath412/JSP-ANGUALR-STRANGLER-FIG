package com.example.legacypoc.service;

import com.example.legacypoc.dao.EmployeeDao;
import com.example.legacypoc.exception.DuplicateEmployeeEmailException;
import com.example.legacypoc.exception.EmployeeNotFoundException;
import com.example.legacypoc.model.Employee;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultEmployeeServiceTest {

    @Mock
    private EmployeeDao employeeDao;
    private DefaultEmployeeService service;

    @Before
    public void setUp() {
        service = new DefaultEmployeeService(employeeDao);
    }

    @Test
    public void getEmployeesDelegatesToDao() {
        when(employeeDao.findAll()).thenReturn(Collections.<Employee>emptyList());
        assertEquals(0, service.getEmployees().size());
        verify(employeeDao).findAll();
    }

    @Test
    public void getEmployeeReturnsExistingEmployee() {
        Employee employee = employee(10L, "one@example.com");
        when(employeeDao.findById(10L)).thenReturn(employee);
        assertSame(employee, service.getEmployee(10L));
    }

    @Test(expected = EmployeeNotFoundException.class)
    public void getEmployeeRejectsMissingEmployee() {
        when(employeeDao.findById(99L)).thenReturn(null);
        service.getEmployee(99L);
    }

    @Test(expected = DuplicateEmployeeEmailException.class)
    public void createRejectsDuplicateEmail() {
        Employee employee = employee(null, "duplicate@example.com");
        when(employeeDao.emailExists("duplicate@example.com")).thenReturn(true);
        service.createEmployee(employee);
    }

    @Test
    public void createNormalizesAndInsertsEmployee() {
        Employee employee = employee(null, " person@example.com ");
        employee.setFirstName(" Jane ");
        when(employeeDao.emailExists("person@example.com")).thenReturn(false);
        service.createEmployee(employee);
        assertEquals("Jane", employee.getFirstName());
        assertEquals("person@example.com", employee.getEmail());
        verify(employeeDao).insert(employee);
    }

    @Test(expected = DuplicateEmployeeEmailException.class)
    public void updateRejectsEmailOwnedByAnotherEmployee() {
        Employee employee = employee(3L, "duplicate@example.com");
        when(employeeDao.findById(3L)).thenReturn(employee);
        when(employeeDao.emailExistsForOtherEmployee("duplicate@example.com", 3L)).thenReturn(true);
        service.updateEmployee(employee);
        verify(employeeDao, never()).update(any(Employee.class));
    }

    @Test
    public void updateDelegatesForExistingUniqueEmployee() {
        Employee employee = employee(3L, "unique@example.com");
        when(employeeDao.findById(3L)).thenReturn(employee);
        when(employeeDao.update(employee)).thenReturn(1);
        service.updateEmployee(employee);
        verify(employeeDao).update(employee);
    }

    @Test(expected = EmployeeNotFoundException.class)
    public void deleteRejectsMissingEmployee() {
        when(employeeDao.delete(55L)).thenReturn(0);
        service.deleteEmployee(55L);
    }

    private Employee employee(Long id, String email) {
        Employee employee = new Employee();
        employee.setEmployeeId(id);
        employee.setFirstName("Jane");
        employee.setLastName("Doe");
        employee.setEmail(email);
        employee.setDepartment("Engineering");
        return employee;
    }
}
