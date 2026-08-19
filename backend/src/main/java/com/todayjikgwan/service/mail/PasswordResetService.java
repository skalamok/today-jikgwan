package com.todayjikgwan.service.mail;

import com.todayjikgwan.common.exception.ApiException;
import com.todayjikgwan.common.exception.ErrorCode;
import com.todayjikgwan.domain.user.User;
import com.todayjikgwan.domain.user.UserRepository;
import com.todayjikgwan.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * REQ-F-004 비밀번호 재설정.
 *
 * <p>이메일 소유 확인과 같은 방식이다. 토큰을 쌓지 않고 서명 토큰으로 만들며 30분이면 만료된다.
 * 용도를 담아 두어 로그인 토큰이나 이메일 확인 토큰을 돌려쓸 수 없게 한다.
 *
 * <p><b>계정이 있는지 알려주지 않는다.</b> 없는 이메일로 요청해도 같은 응답을 준다.
 * 응답이 갈리면 그것만으로 가입 여부를 캐낼 수 있다 (REQ-NF-008).
 */
@Service
public class PasswordResetService {

    private static final String PURPOSE = "password-reset";
    private static final long TTL_SECONDS = 30 * 60;      // REQ-F-004 링크 유효기간 30분

    private final SecretKey key;
    private final UserRepository userRepository;
    private final MailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final String frontendBaseUrl;

    public PasswordResetService(JwtProperties jwtProperties, UserRepository userRepository,
                                MailSender mailSender, PasswordEncoder passwordEncoder,
                                @Value("${todayjikgwan.frontend-base-url:http://localhost:5173}")
                                String frontendBaseUrl) {
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    /** 가입된 이메일일 때만 실제로 보낸다. 부르는 쪽은 결과를 알 수 없다 */
    public void request(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            Date now = new Date();
            String token = Jwts.builder()
                    .subject(String.valueOf(user.getId()))
                    .claim("purpose", PURPOSE)
                    .issuedAt(now)
                    .expiration(new Date(now.getTime() + TTL_SECONDS * 1000))
                    .signWith(key)
                    .compact();
            mailSender.send(email, "[오늘의직관] 비밀번호를 다시 정해 주세요", """
                    아래 링크에서 새 비밀번호를 정할 수 있습니다. 30분이 지나면 만료됩니다.

                    %s/reset-password?token=%s

                    본인이 요청한 것이 아니라면 이 메일을 지우면 됩니다.
                    """.formatted(frontendBaseUrl, token));
        });
    }

    @Transactional
    public void reset(String token, String newPassword) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INVALID_VERIFICATION_TOKEN);
        }
        if (!PURPOSE.equals(claims.get("purpose", String.class))) {
            throw new ApiException(ErrorCode.INVALID_VERIFICATION_TOKEN);
        }
        User user = userRepository.findById(Long.valueOf(claims.getSubject()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_VERIFICATION_TOKEN));
        user.changePassword(passwordEncoder.encode(newPassword));
    }
}
