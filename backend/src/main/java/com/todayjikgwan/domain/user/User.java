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

    public void promoteToAdmin() { this.role = UserRole.ADMIN; }

    public void changeFavoriteTeam(Team team) { this.favoriteTeam = team; }

    public void changeNickname(String nickname) { this.nickname = nickname; }

    /** REQ-F-001 이메일 소유 확인. 이미 확인한 계정은 시각을 덮어쓰지 않는다 */
    public void verifyEmail() {
        if (this.emailVerifiedAt == null) {
            this.emailVerifiedAt = OffsetDateTime.now();
        }
    }

    public void changePassword(String encoded) { this.passwordHash = encoded; }

    /**
     * REQ-F-007 탈퇴.
     *
     * <p>행을 지우지 않는다. 이 사람이 남긴 구역 평가가 다른 사람의 좌석 선택 근거이므로
     * 통째로 지우면 집계가 흔들린다. 식별할 수 있는 값만 지우고 기여분은 익명으로 남긴다.
     * 같은 이메일로 다시 가입할 수 있도록 이메일도 비운다.
     */
    public void withdraw() {
        this.withdrawnAt = OffsetDateTime.now();
        this.status = UserStatus.WITHDRAWN;
        this.email = null;
        this.passwordHash = null;
        this.nickname = "탈퇴한 사용자" + this.id;
        this.profileImageUrl = null;
        this.favoriteTeam = null;
    }
}
