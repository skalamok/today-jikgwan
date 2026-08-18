package com.todayjikgwan.domain.companion;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 읽음 처리.
 *
 * <p>메시지마다 읽음 행을 쌓으면 메시지 수 × 참여자 수만큼 행이 늘어난다.
 * 사용자별 <b>마지막 읽은 시각</b>만 보관하고 미읽음 개수는 그 이후 메시지를 세어 구한다.
 */
@Getter
@Entity
@Table(name = "companion_message_reads")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanionMessageRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "companion_post_id", nullable = false)
    private Long companionPostId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "last_read_at", nullable = false)
    private OffsetDateTime lastReadAt = OffsetDateTime.now();

    public CompanionMessageRead(Long postId, Long userId) {
        this.companionPostId = postId;
        this.userId = userId;
    }

    public void touch() { this.lastReadAt = OffsetDateTime.now(); }
}
