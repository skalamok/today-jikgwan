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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일 소유 확인 (REQ-F-001).
 *
 * <p>토큰을 테이블에 쌓지 않고 서명 토큰으로 만든다. 유효기간이 30분뿐이라 보관해서
 * 얻을 것이 적고, 예전에 두었다가 쓰지 않아 지운 auth_tokens 를 되살릴 이유도 없다.
 * 로그인 토큰과 섞이지 않게 용도를 claim 으로 박고 검증에서 확인한다.
 */
@Slf4j
@Service
public class EmailVerificationService {

    private static final String PURPOSE = "email-verify";
    private static final long TTL_SECONDS = 30 * 60;      // REQ-F-001 링크 유효기간 30분

    private final SecretKey key;
    private final UserRepository userRepository;
    private final MailSender mailSender;
    private final String frontendBaseUrl;

    public EmailVerificationService(JwtProperties jwtProperties, UserRepository userRepository,
                                    MailSender mailSender,
                                    @Value("${todayjikgwan.frontend-base-url:http://localhost:5173}")
                                    String frontendBaseUrl) {
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    /** 가입 직후 부른다. 발송에 실패해도 가입 자체는 되돌리지 않는다 */
    public void sendVerification(User user) {
        String token = issue(user.getId());
        String link = "%s/verify-email?token=%s".formatted(frontendBaseUrl, token);
        mailSender.send(user.getEmail(), "[오늘의직관] 이메일 주소를 확인해 주세요", """
                가입해 주셔서 고맙습니다.
                아래 링크를 열면 이메일 확인이 끝납니다. 30분이 지나면 만료됩니다.

                %s
                """.formatted(link));
    }

    @Transactional
    public void verify(String token) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            // 만료 · 위조 · 형식 오류를 구분하지 않는다. 어느 쪽이든 다시 받아야 한다
            throw new ApiException(ErrorCode.INVALID_VERIFICATION_TOKEN);
        }
        if (!PURPOSE.equals(claims.get("purpose", String.class))) {
            throw new ApiException(ErrorCode.INVALID_VERIFICATION_TOKEN);
        }
        User user = userRepository.findById(Long.valueOf(claims.getSubject()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_VERIFICATION_TOKEN));
        user.verifyEmail();
    }

    private String issue(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("purpose", PURPOSE)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + TTL_SECONDS * 1000))
                .signWith(key)
                .compact();
    }
}
