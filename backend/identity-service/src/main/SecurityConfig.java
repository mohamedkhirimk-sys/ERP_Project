package com.erp.system.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disable CSRF (not needed for JWT/Stateless APIs)
            .csrf(csrf -> csrf.disable())
            
            // 2. Set Session Policy to Stateless (JWT doesn't use sessions)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 3. Configure Authorization Rules
            .authorizeHttpRequests(auth -> auth
                // Allow all requests to the Auth and Identity endpoints (Login/Register)
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/identity/**").permitAll()
                
                // All other requests must be authenticated via JWT
                .anyRequest().authenticated()
            );

        return http.build();
    }
}

