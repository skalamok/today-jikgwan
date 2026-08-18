package com.todayjikgwan.domain.badge;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** REQ-F-703. 기록 1건만으로도 성취가 발생하도록 설계한다 */
@Getter
@Entity
@Table(name = "user_badges")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "achieved_at", nullable = false)
    private OffsetDateTime achievedAt = OffsetDateTime.now();

    public UserBadge(Long userId, Badge badge) {
        this.userId = userId;
        this.badge = badge;
    }
}
