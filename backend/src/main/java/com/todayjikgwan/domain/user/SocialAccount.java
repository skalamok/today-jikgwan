package com.todayjikgwan.domain.user;

import com.todayjikgwan.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_social_accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Column(length = 255)
    private String email;

    @Column(name = "linked_at", nullable = false)
    private OffsetDateTime linkedAt = OffsetDateTime.now();

    public static SocialAccount link(User user, OAuthProvider provider, String providerUserId, String email) {
        SocialAccount a = new SocialAccount();
        a.user = user;
        a.provider = provider;
        a.providerUserId = providerUserId;
        a.email = email;
        a.linkedAt = OffsetDateTime.now();
        return a;
    }
}
