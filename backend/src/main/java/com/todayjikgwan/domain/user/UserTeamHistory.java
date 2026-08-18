package com.todayjikgwan.domain.user;

import com.todayjikgwan.domain.team.Team;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** REQ-F-005 응원팀 변경 이력 */
@Getter
@Entity
@Table(name = "user_team_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTeamHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt = OffsetDateTime.now();

    public UserTeamHistory(User user, Team team) {
        this.user = user;
        this.team = team;
    }
}
