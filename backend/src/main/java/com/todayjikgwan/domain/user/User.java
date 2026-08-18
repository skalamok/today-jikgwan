package com.todayjikgwan.domain.user;

import com.todayjikgwan.common.entity.BaseTimeEntity;
import com.todayjikgwan.domain.team.Team;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 30, unique = true)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /** 현재 응원팀. 이미 작성된 기록의 승패 판정에는 소급하지 않는다 (REQ-F-005) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "favorite_team_id")
    private Team favoriteTeam;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "email_verified_at")
    private OffsetDateTime emailVerifiedAt;

    @Column(name = "withdrawn_at")
    private OffsetDateTime withdrawnAt;

    @Builder
    private User(String email, String passwordHash, String nickname, Team favoriteTeam) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.favoriteTeam = favoriteTeam;
    }

    public void changeFavoriteTeam(Team team) { this.favoriteTeam = team; }

    public void changeNickname(String nickname) { this.nickname = nickname; }
}
