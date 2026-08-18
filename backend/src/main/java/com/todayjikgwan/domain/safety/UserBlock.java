package com.todayjikgwan.domain.safety;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** REQ-F-509 사용자 차단 */
@Getter
@Entity
@Table(name = "user_blocks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "blocked_id", nullable = false)
    private Long blockedId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public UserBlock(Long userId, Long blockedId) {
        this.userId = userId;
        this.blockedId = blockedId;
    }
}
