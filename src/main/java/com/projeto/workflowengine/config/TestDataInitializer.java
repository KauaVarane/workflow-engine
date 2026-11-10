package com.projeto.workflowengine.config;

import com.projeto.workflowengine.core.entity.user.User;
import com.projeto.workflowengine.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Profile("test")
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername("test").isEmpty()) {
            User user = User.builder()
                    .username("test")
                    .password(passwordEncoder.encode("test"))
                    .roles(List.of("USER"))
                    .build();
            userRepository.save(user);
        }
    }
}