package com.todayjikgwan.domain.companion;

import com.todayjikgwan.domain.game.Game;
import com.todayjikgwan.domain.user.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메이트 모집글 (REQ-F-501).
 *
 * <p>동시성 제어의 핵심 지점이다. 여러 사용자가 동시에 참여를 누르면
 * confirmedCount 가 capacity 를 넘을 수 있으므로 세 겹으로 막는다.
 * <ol>
 *   <li>애플리케이션: 정원 검사</li>
 *   <li>낙관적 락(version): 동시 갱신 감지 후 재시도 (REQ-NF-011)</li>
 *   <li>DB CHECK 제약(ck_post_capacity): 최종 방어선</li>
 * </ol>
 */
@Getter
@Entity
@Table(name = "companion_posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanionPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(length = 500)
    private String intro;

    /** 작성자를 포함한 정원 */
    @Column(nullable = false)
    private int capacity;

    @Column(name = "confirmed_count", nullable = false)
    private int confirmedCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status = PostStatus.OPEN;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    public CompanionPost(Game game, User author, int capacity, String intro) {
        this.game = game;
        this.author = author;
        this.capacity = capacity;
        this.intro = intro;
    }

    public boolean isFull() {
        return confirmedCount >= capacity;
    }

    public boolean isOpen() {
        return status == PostStatus.OPEN && !isFull();
    }

    /** 확정 1명을 추가하고 정원이 차면 상태를 FULL 로 전환한다. */
    public int confirmOne() {
        if (isFull()) {
            throw new IllegalStateException("정원 초과");
        }
        this.confirmedCount++;
        if (isFull()) {
            this.status = PostStatus.FULL;
        }
        return this.confirmedCount;
    }

    /** 확정자가 취소하면 자리를 다시 모집 상태로 되돌린다 (REQ-F-506). */
    public void releaseOne() {
        this.confirmedCount--;
        if (this.status == PostStatus.FULL) {
            this.status = PostStatus.OPEN;
        }
    }

    public void close() {
        this.status = PostStatus.CLOSED;
        this.closedAt = OffsetDateTime.now();
    }
}
