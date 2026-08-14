package com.example.legacypoc.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AntPathRequestMatcher apiRequest = new AntPathRequestMatcher("/api/**");

        http.authorizeHttpRequests(authorize -> authorize
                .antMatchers("/login", "/resources/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/**").hasAnyRole("EMPLOYEE_VIEWER", "EMPLOYEE_ADMIN")
                .antMatchers("/api/**").hasRole("EMPLOYEE_ADMIN")
                .antMatchers(HttpMethod.POST, "/employees/**").hasRole("EMPLOYEE_ADMIN")
                .antMatchers("/", "/employees/**", "/app", "/app/**")
                    .hasAnyRole("EMPLOYEE_VIEWER", "EMPLOYEE_ADMIN")
                .anyRequest().denyAll())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", false)
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID"))
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(new RestAuthenticationEntryPoint(), apiRequest)
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new NegatedRequestMatcher(apiRequest))
                .defaultAccessDeniedHandlerFor(new RestAccessDeniedHandler(), apiRequest));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.withUsername("employee-admin")
                .password(passwordEncoder.encode("admin-demo"))
                .roles("EMPLOYEE_ADMIN", "EMPLOYEE_VIEWER")
                .build();
        UserDetails viewer = User.withUsername("employee-viewer")
                .password(passwordEncoder.encode("viewer-demo"))
                .roles("EMPLOYEE_VIEWER")
                .build();
        return new InMemoryUserDetailsManager(admin, viewer);
    }
}
