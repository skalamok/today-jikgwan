package com.todayjikgwan.service;

import com.todayjikgwan.api.auth.dto.LoginRequest;
import com.todayjikgwan.api.auth.dto.SignupRequest;
import com.todayjikgwan.api.auth.dto.TokenResponse;
import com.todayjikgwan.common.exception.ApiException;
import com.todayjikgwan.common.exception.ErrorCode;
import com.todayjikgwan.domain.team.Team;
import com.todayjikgwan.domain.team.TeamRepository;
import com.todayjikgwan.domain.user.User;
import com.todayjikgwan.domain.user.UserRepository;
import com.todayjikgwan.domain.user.UserTeamHistory;
import com.todayjikgwan.domain.user.UserTeamHistoryRepository;
import com.todayjikgwan.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserTeamHistoryRepository teamHistoryRepository;
    private final com.todayjikgwan.service.mail.EmailVerificationService emailVerificationService;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /** REQ-F-001 */
    @Transactional
    public Long signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new ApiException(ErrorCode.DUPLICATE_NICKNAME);
        }
        Team team = request.favoriteTeamId() == null ? null
                : teamRepository.findById(request.favoriteTeamId())
                        .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .favoriteTeam(team)
                .build();
        User saved = userRepository.save(user);
        // REQ-F-005 가입 때 고른 팀이 이력의 시작점이다. 나중에 바꾸면 그 위에 쌓인다
        if (team != null) {
            teamHistoryRepository.save(new UserTeamHistory(saved, team));
        }
        // REQ-F-001 이메일 소유 확인. 발송이 실패해도 가입을 되돌리지 않는다
        emailVerificationService.sendVerification(saved);
        return saved.getId();
    }

    /**
     * REQ-F-002.
     * 이메일과 비밀번호 중 무엇이 틀렸는지 구분하여 안내하지 않는다(계정 존재 여부 노출 방지).
     */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        String token = jwtProvider.createAccessToken(user.getId(), user.getRole().name());
        return new TokenResponse(token, jwtProvider.getAccessTokenSeconds());
    }
}
