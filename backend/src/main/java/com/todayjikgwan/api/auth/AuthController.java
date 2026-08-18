package com.todayjikgwan.api.auth;

import com.todayjikgwan.api.auth.dto.LoginRequest;
import com.todayjikgwan.api.auth.dto.SignupRequest;
import com.todayjikgwan.api.auth.dto.TokenResponse;
import com.todayjikgwan.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Long>> signup(@Valid @RequestBody SignupRequest request) {
        Long id = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("userId", id));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
