package com.projeto.workflowengine.controller;

import com.projeto.workflowengine.dto.auth.AuthRequest;
import com.projeto.workflowengine.dto.auth.AuthResponse;
import com.projeto.workflowengine.security.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        String token = authService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .username(request.getUsername())
                .build());
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody AuthRequest request) {
        authService.register(request.getUsername(), request.getPassword());
        return ResponseEntity.ok("Usuário registrado com sucesso!");
    }
}
