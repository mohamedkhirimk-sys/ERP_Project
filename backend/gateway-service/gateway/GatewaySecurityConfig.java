package com.erp.system.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter; // Note: For Spring Boot 3, use SecurityFilterChain
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable() // Disable CSRF for APIs
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS) // No sessions, only JWT
            .and()
            .authorizeHttpRequests()
                // Allow everyone to access the login/register endpoints of Identity Service
                .requestMatchers("/auth/**").permitAll() 
                // All other requests must be authenticated via JWT
                .anyRequest().authenticated();

        return http.build();
    }
}
