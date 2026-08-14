package com.example.legacypoc.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Controller
public class AuthenticationController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/api/session")
    @ResponseBody
    public UserSession currentSession(Authentication authentication, HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            csrfToken = (CsrfToken) request.getAttribute("_csrf");
        }

        List<String> roles = new ArrayList<String>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            roles.add(authority.getAuthority());
        }
        return new UserSession(authentication.getName(), roles,
                csrfToken.getHeaderName(), csrfToken.getToken());
    }

    public static class UserSession {
        private final String username;
        private final List<String> roles;
        private final String csrfHeaderName;
        private final String csrfToken;

        public UserSession(String username, List<String> roles,
                           String csrfHeaderName, String csrfToken) {
            this.username = username;
            this.roles = roles;
            this.csrfHeaderName = csrfHeaderName;
            this.csrfToken = csrfToken;
        }

        public String getUsername() { return username; }
        public List<String> getRoles() { return roles; }
        public String getCsrfHeaderName() { return csrfHeaderName; }
        public String getCsrfToken() { return csrfToken; }
    }
}
