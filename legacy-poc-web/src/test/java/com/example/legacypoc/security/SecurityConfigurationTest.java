package com.example.legacypoc.security;

import com.example.legacypoc.controller.AuthenticationController;
import com.example.legacypoc.controller.EmployeeController;
import com.example.legacypoc.controller.EmployeeRestController;
import com.example.legacypoc.service.EmployeeService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SecurityConfigurationTest {
    private AnnotationConfigApplicationContext context;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        context = new AnnotationConfigApplicationContext(SecurityConfiguration.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        when(employeeService.getEmployees()).thenReturn(Collections.emptyList());
        FilterChainProxy securityFilter = context.getBean(FilterChainProxy.class);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");
        mockMvc = MockMvcBuilders.standaloneSetup(
                new EmployeeController(employeeService),
                new EmployeeRestController(employeeService),
                new AuthenticationController())
                .apply(springSecurity(securityFilter))
                .setViewResolvers(viewResolver)
                .build();
    }

    @After
    public void tearDown() {
        context.close();
    }

    @Test
    public void unauthenticatedUiRedirectsButApiReturnsJson401() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    public void oneLoginSessionAuthorizesJspAndRest() throws Exception {
        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "employee-admin")
                        .param("password", "admin-demo"))
                .andExpect(status().isFound())
                .andExpect(authenticated().withUsername("employee-admin"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/employees").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/api/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("employee-admin"))
                .andExpect(jsonPath("$.csrfToken").isNotEmpty());
    }

    @Test
    public void viewerCannotWriteEvenWithValidCsrf() throws Exception {
        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "employee-viewer")
                        .param("password", "viewer-demo"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(post("/api/employees").session(session).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(
                        "You do not have permission to perform this operation."));
    }
}
