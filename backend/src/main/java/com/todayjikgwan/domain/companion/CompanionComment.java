package com.todayjikgwan.domain.companion;

import com.todayjikgwan.domain.user.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** REQ-F-510 모집글 공개 문의 댓글 */
@Getter
@Entity
@Table(name = "companion_comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanionComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "companion_post_id", nullable = false)
    private CompanionPost companionPost;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public CompanionComment(CompanionPost post, User user, String content) {
        this.companionPost = post;
        this.user = user;
        this.content = content;
    }

    public void delete() { this.deletedAt = OffsetDateTime.now(); }
}
