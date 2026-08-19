package com.todayjikgwan.api.auth;

import com.todayjikgwan.api.auth.dto.LoginRequest;
import com.todayjikgwan.api.auth.dto.SignupRequest;
import com.todayjikgwan.api.auth.dto.TokenResponse;
import com.todayjikgwan.service.AuthService;
import com.todayjikgwan.service.mail.EmailVerificationService;
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
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Long>> signup(@Valid @RequestBody SignupRequest request) {
        Long id = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("userId", id));
    }

    /**
     * REQ-F-001 이메일 소유 확인.
     *
     * 가입 때 보낸 링크가 이 경로로 들어온다. 링크는 30분이 지나면 만료된다.
     */
    @PostMapping("/verify-email")
    public Map<String, String> verifyEmail(@RequestBody Map<String, String> body) {
        emailVerificationService.verify(body.get("token"));
        return Map.of("message", "이메일 확인이 끝났어요");
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
