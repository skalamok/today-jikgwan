package com.todayjikgwan.service.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 기본 구현체. 메일을 보내지 않고 로그로 남긴다 (REQ-F-001).
 *
 * <p>발송 서버가 없는 상태에서 인증 흐름 자체는 확인할 수 있어야 한다.
 * 로그에 찍힌 링크를 그대로 열면 실제 메일을 받은 것과 같은 경로를 지난다.
 *
 * <p><b>운영에서는 쓰지 않는다.</b> 인증 링크가 서버 로그에 그대로 남기 때문이다.
 */
@Slf4j
@Component
public class LoggingMailSender implements MailSender {

    @Override
    public void send(String to, String subject, String body) {
        log.info("""

                ── 메일 (실제로 보내지 않음) ─────────────────────
                받는 사람 : {}
                제목      : {}
                {}
                ────────────────────────────────────────────────
                """, to, subject, body);
    }

    @Override
    public String displayName() {
        return "로그 출력 (발송하지 않음)";
    }
}
