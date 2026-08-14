package com.example.legacypoc.controller;

import com.example.legacypoc.exception.DuplicateEmployeeEmailException;
import com.example.legacypoc.model.Employee;
import com.example.legacypoc.service.EmployeeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/employees")
    public String listEmployees(Model model) {
        model.addAttribute("employees", employeeService.getEmployees());
        return "employees";
    }

    @GetMapping("/employees/new")
    public String newEmployee(Model model) {
        model.addAttribute("employee", new Employee());
        prepareForm(model, false, null);
        return "employee-form";
    }

    @PostMapping("/employees")
    public String createEmployee(@Valid @ModelAttribute("employee") Employee employee,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, false, null);
            return "employee-form";
        }
        try {
            employeeService.createEmployee(employee);
        } catch (DuplicateEmployeeEmailException exception) {
            bindingResult.rejectValue("email", "employee.email.duplicate",
                    "An employee with this email already exists");
            prepareForm(model, false, null);
            return "employee-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Employee created successfully.");
        return "redirect:/employees";
    }

    @GetMapping("/employees/{id}/edit")
    public String editEmployee(@PathVariable("id") Long id, Model model) {
        model.addAttribute("employee", employeeService.getEmployee(id));
        prepareForm(model, true, id);
        return "employee-form";
    }

    @PostMapping("/employees/{id}")
    public String updateEmployee(@PathVariable("id") Long id,
                                 @Valid @ModelAttribute("employee") Employee employee,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        employee.setEmployeeId(id);
        if (bindingResult.hasErrors()) {
            prepareForm(model, true, id);
            return "employee-form";
        }
        try {
            employeeService.updateEmployee(employee);
        } catch (DuplicateEmployeeEmailException exception) {
            bindingResult.rejectValue("email", "employee.email.duplicate",
                    "An employee with this email already exists");
            prepareForm(model, true, id);
            return "employee-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Employee updated successfully.");
        return "redirect:/employees";
    }

    @PostMapping("/employees/{id}/delete")
    public String deleteEmployee(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        employeeService.deleteEmployee(id);
        redirectAttributes.addFlashAttribute("successMessage", "Employee deleted successfully.");
        return "redirect:/employees";
    }

    private void prepareForm(Model model, boolean editing, Long id) {
        model.addAttribute("formTitle", editing ? "Edit Employee" : "Add Employee");
        model.addAttribute("formAction", editing ? "/employees/" + id : "/employees");
    }
}
