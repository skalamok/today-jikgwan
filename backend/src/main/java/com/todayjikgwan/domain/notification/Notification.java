package com.todayjikgwan.domain.notification;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** REQ-F-604, REQ-F-706 */
@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** GAME_REVISED / COMPANION_CONFIRMED / BADGE_ACHIEVED */
    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 255)
    private String body;

    @Column(name = "link_url", length = 255)
    private String linkUrl;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Notification(Long userId, String type, String title, String body, String linkUrl) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.linkUrl = linkUrl;
    }

    public void read() { this.readAt = OffsetDateTime.now(); }
}
