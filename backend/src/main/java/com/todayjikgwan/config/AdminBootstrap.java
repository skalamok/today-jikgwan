package com.todayjikgwan.config;

import com.todayjikgwan.domain.user.User;
import com.todayjikgwan.domain.user.UserRepository;
import com.todayjikgwan.domain.user.UserRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영자 지정.
 *
 * <p>권한 부여를 DB 직접 수정으로 하지 않기 위해 둔다. 지정된 이메일로 <b>이미 가입한</b>
 * 계정만 승격하며, 계정을 새로 만들지는 않는다. 목록에서 빠진다고 강등하지도 않는다
 * (운영 중 설정 실수로 권한이 사라지는 편이 더 위험하다).
 *
 * <p>{@code run} 은 컨테이너가 프록시를 통해 부르므로 트랜잭션이 걸린다.
 * 다른 메서드에서 자기 자신을 호출하면 프록시를 타지 않아 변경이 커밋되지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;

    @Value("${todayjikgwan.admin-emails:}")
    private List<String> adminEmails;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String email : adminEmails) {
            if (email == null || email.isBlank()) {
                continue;
            }
            userRepository.findByEmail(email.trim()).ifPresentOrElse(
                    this::toAdmin,
                    () -> log.warn("운영자로 지정된 {} 계정이 없습니다. 먼저 가입해야 합니다.", email));
        }
    }

    private void toAdmin(User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        user.promoteToAdmin();
        log.info("{} 을(를) 운영자로 지정했습니다.", user.getEmail());
    }
}
