package com.example.legacypoc.controller;

import com.example.legacypoc.exception.DuplicateEmployeeEmailException;
import com.example.legacypoc.exception.EmployeeNotFoundException;
import com.example.legacypoc.model.Employee;
import com.example.legacypoc.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.sql.Timestamp;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class EmployeeRestControllerTest {

    @Mock
    private EmployeeService employeeService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EmployeeRestController(employeeService))
                .setControllerAdvice(new RestExceptionHandler())
                .setValidator(new RequiredEmployeeFieldsValidator())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void listReturnsJsonEmployees() throws Exception {
        when(employeeService.getEmployees()).thenReturn(Collections.singletonList(employee(7L)));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].employeeId", is(7)))
                .andExpect(jsonPath("$[0].email", is("jane@example.com")));
    }

    @Test
    public void getMissingEmployeeReturnsJson404() throws Exception {
        when(employeeService.getEmployee(99L)).thenThrow(new EmployeeNotFoundException(99L));

        mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("EMPLOYEE_NOT_FOUND")));
    }

    @Test
    public void createReturns201AndIgnoresClientId() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(employee(22L))))
                .andExpect(status().isCreated());

        verify(employeeService).createEmployee(any(Employee.class));
    }

    @Test
    public void updateReturns204AndUsesPathId() throws Exception {
        mockMvc.perform(put("/api/employees/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(employee(22L))))
                .andExpect(status().isNoContent());

        verify(employeeService).updateEmployee(any(Employee.class));
    }

    @Test
    public void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/employees/8"))
                .andExpect(status().isNoContent());
        verify(employeeService).deleteEmployee(8L);
    }

    @Test
    public void duplicateEmailReturns409WithFieldError() throws Exception {
        doThrow(new DuplicateEmployeeEmailException("jane@example.com"))
                .when(employeeService).createEmployee(any(Employee.class));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(employee(null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_EMAIL")))
                .andExpect(jsonPath("$.fieldErrors.email", is("An employee with this email already exists")));
    }

    @Test
    public void invalidPayloadReturns400WithFieldErrors() throws Exception {
        Employee invalid = employee(null);
        invalid.setFirstName("");
        invalid.setLastName("");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.lastName").exists());
    }

    private Employee employee(Long id) {
        Employee employee = new Employee();
        employee.setEmployeeId(id);
        employee.setFirstName("Jane");
        employee.setLastName("Doe");
        employee.setEmail("jane@example.com");
        employee.setDepartment("Engineering");
        employee.setCreatedAt(Timestamp.valueOf("2026-08-12 12:00:00"));
        return employee;
    }

    private static class RequiredEmployeeFieldsValidator implements Validator {
        @Override
        public boolean supports(Class<?> type) {
            return Employee.class.isAssignableFrom(type);
        }

        @Override
        public void validate(Object target, Errors errors) {
            Employee employee = (Employee) target;
            if (employee.getFirstName() == null || employee.getFirstName().trim().isEmpty()) {
                errors.rejectValue("firstName", "required", "First name is required");
            }
            if (employee.getLastName() == null || employee.getLastName().trim().isEmpty()) {
                errors.rejectValue("lastName", "required", "Last name is required");
            }
        }
    }
}
