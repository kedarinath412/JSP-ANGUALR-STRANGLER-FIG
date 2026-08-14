package com.example.legacypoc.controller;

import com.example.legacypoc.exception.DuplicateEmployeeEmailException;
import com.example.legacypoc.exception.EmployeeNotFoundException;
import com.example.legacypoc.model.Employee;
import com.example.legacypoc.service.EmployeeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@RunWith(MockitoJUnitRunner.class)
public class EmployeeControllerTest {

    @Mock
    private EmployeeService employeeService;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        EmployeeController controller = new EmployeeController(employeeService);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new RequiredEmployeeFieldsValidator())
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    public void homeDisplaysHomeView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }

    @Test
    public void listDisplaysEmployees() throws Exception {
        when(employeeService.getEmployees()).thenReturn(Collections.<Employee>emptyList());
        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees"))
                .andExpect(model().attributeExists("employees"));
    }

    @Test
    public void newFormProvidesEmployeeAndCreateAction() throws Exception {
        mockMvc.perform(get("/employees/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-form"))
                .andExpect(model().attributeExists("employee"))
                .andExpect(model().attribute("formAction", "/employees"));
    }

    @Test
    public void validCreateRedirectsWithSuccessMessage() throws Exception {
        mockMvc.perform(validEmployee(post("/employees")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"))
                .andExpect(flash().attributeExists("successMessage"));
        verify(employeeService).createEmployee(any(Employee.class));
    }

    @Test
    public void invalidCreateReturnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/employees").param("email", "valid@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-form"))
                .andExpect(model().attributeHasFieldErrors("employee", "firstName", "lastName"));
    }

    @Test
    public void duplicateCreateReturnsFriendlyFieldError() throws Exception {
        doThrow(new DuplicateEmployeeEmailException("jane@example.com"))
                .when(employeeService).createEmployee(any(Employee.class));
        mockMvc.perform(validEmployee(post("/employees")))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-form"))
                .andExpect(model().attributeHasFieldErrors("employee", "email"));
    }

    @Test
    public void editLoadsEmployee() throws Exception {
        when(employeeService.getEmployee(7L)).thenReturn(employee(7L));
        mockMvc.perform(get("/employees/7/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-form"))
                .andExpect(model().attribute("formAction", "/employees/7"));
    }

    @Test
    public void validUpdateRedirects() throws Exception {
        mockMvc.perform(validEmployee(post("/employees/7")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"));
        verify(employeeService).updateEmployee(any(Employee.class));
    }

    @Test
    public void deleteUsesPostAndRedirects() throws Exception {
        mockMvc.perform(post("/employees/7/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"));
        verify(employeeService).deleteEmployee(7L);
    }

    @Test
    public void missingEmployeeUsesFriendly404View() throws Exception {
        when(employeeService.getEmployee(99L)).thenThrow(new EmployeeNotFoundException(99L));
        mockMvc.perform(get("/employees/99/edit"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("errorTitle", "Employee Not Found"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validEmployee(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) {
        return request.param("firstName", "Jane")
                .param("lastName", "Doe")
                .param("email", "jane@example.com")
                .param("department", "Engineering");
    }

    private Employee employee(Long id) {
        Employee employee = new Employee();
        employee.setEmployeeId(id);
        employee.setFirstName("Jane");
        employee.setLastName("Doe");
        employee.setEmail("jane@example.com");
        return employee;
    }

    private static class RequiredEmployeeFieldsValidator implements Validator {
        @Override
        public boolean supports(Class<?> type) { return Employee.class.isAssignableFrom(type); }

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
