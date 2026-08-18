package com.todayjikgwan.domain.companion;

import com.todayjikgwan.domain.user.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * REQ-F-511 확정자 전용 대화.
 *
 * <p>모집 글 1건에 대화방이 정확히 1개 대응하므로 중간 테이블을 두지 않고
 * 모집 글을 직접 참조한다. 방을 따로 두면 "글은 있는데 방이 없는" 상태가 만들어질 수 있다.
 */
@Getter
@Entity
@Table(name = "companion_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanionMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "companion_post_id", nullable = false)
    private CompanionPost companionPost;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public CompanionMessage(CompanionPost post, User user, String content) {
        this.companionPost = post;
        this.user = user;
        this.content = content;
    }
}
