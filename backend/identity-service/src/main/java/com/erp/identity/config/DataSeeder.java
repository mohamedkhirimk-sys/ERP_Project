package com.erp.identity.config;

import com.erp.common.security.Role;
import com.erp.identity.entity.User;
import com.erp.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Users already exist, skipping seed");
            return;
        }

        User admin = User.builder()
                .username("admin")
                .email("admin@erp.local")
                .fullName("Admin User")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);
        log.info("Created default admin user (admin / admin123)");

        User staff = User.builder()
                .username("staff")
                .email("staff@erp.local")
                .fullName("Staff User")
                .password(passwordEncoder.encode("staff123"))
                .role(Role.MANAGER)
                .build();
        userRepository.save(staff);
        log.info("Created default staff user (staff / staff123)");
    }
}
