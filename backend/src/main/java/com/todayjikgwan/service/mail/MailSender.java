package com.todayjikgwan.service.mail;

/**
 * 메일 발송 추상화 (REQ-F-001, REQ-F-004).
 *
 * <p>보낼 메일 서버를 아직 정하지 않았다. 학습용 프로젝트라 도메인과 발송 계정이 없고,
 * 그것 없이는 어느 서비스를 쓸지도 정할 수 없다. 그래서 발송 지점만 인터페이스로 끊어 두고
 * 기본 구현체는 로그로 남긴다. 나중에 SMTP 든 외부 발송 서비스든 구현체만 더하면 된다.
 *
 * <p>경기 데이터(GameDataProvider)와 같은 이유의 추상화다.
 */
public interface MailSender {

    void send(String to, String subject, String body);

    /** 사람이 읽을 이름. 어떤 경로로 나갔는지 알리는 데 쓴다 */
    String displayName();
}
