package com.todayjikgwan.domain.companion;

import com.todayjikgwan.domain.user.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** REQ-F-503 ~ 506. 대기자 개념은 두지 않는다. */
@Getter
@Entity
@Table(name = "companion_applications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanionApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "companion_post_id", nullable = false)
    private CompanionPost companionPost;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.CONFIRMED;

    /** 확정 순번. 선착순의 근거로 남긴다. */
    @Column(nullable = false)
    private int seq;

    @Column(name = "applied_at", nullable = false)
    private OffsetDateTime appliedAt = OffsetDateTime.now();

    @Column(name = "canceled_at")
    private OffsetDateTime canceledAt;

    public CompanionApplication(CompanionPost post, User user, int seq) {
        this.companionPost = post;
        this.user = user;
        this.seq = seq;
    }

    public void cancel() {
        this.status = ApplicationStatus.CANCELED;
        this.canceledAt = OffsetDateTime.now();
    }
}
