package com.todayjikgwan.domain.game;

import com.todayjikgwan.common.entity.BaseTimeEntity;
import com.todayjikgwan.domain.team.Stadium;
import com.todayjikgwan.domain.team.Team;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "games")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "season_year", nullable = false)
    private int seasonYear;

    @Column(name = "game_date", nullable = false)
    private LocalDate gameDate;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stadium_id", nullable = false)
    private Stadium stadium;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameStatus status = GameStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameSource source = GameSource.MANUAL;

    @Column(name = "external_ref", length = 100)
    private String externalRef;

    @Column(name = "synced_at")
    private OffsetDateTime syncedAt;

    /** REQ-F-608 미확정 경기는 화면에서 "확인 중"으로 구분 표시한다 */
    @Column(name = "result_confirmed", nullable = false)
    private boolean resultConfirmed = false;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    public boolean isResultConfirmed() {
        return resultConfirmed && homeScore != null && awayScore != null;
    }

    public boolean isStarted() {
        return startAt.isBefore(OffsetDateTime.now());
    }

    public boolean hasTeam(Long teamId) {
        return homeTeam.getId().equals(teamId) || awayTeam.getId().equals(teamId);
    }

    /**
     * 응원팀 기준 승패를 판정한다.
     * cheerTeamId 가 null 이면 중립 관람이므로 집계에서 제외한다 (REQ-F-202).
     */
    public GameResult resultFor(Long cheerTeamId) {
        if (cheerTeamId == null || !resultConfirmed || homeScore == null || awayScore == null) {
            return GameResult.NEUTRAL;
        }
        if (homeScore.equals(awayScore)) {
            return GameResult.DRAW;
        }
        boolean homeWon = homeScore > awayScore;
        boolean cheeredHome = homeTeam.getId().equals(cheerTeamId);
        return homeWon == cheeredHome ? GameResult.WIN : GameResult.LOSE;
    }

    /** REQ-F-607 제보 일치로 결과를 확정한다. */
    public void confirmResult(int home, int away, GameSource by) {
        this.homeScore = home;
        this.awayScore = away;
        this.status = GameStatus.FINISHED;
        this.source = by;
        this.resultConfirmed = true;
        this.confirmedAt = OffsetDateTime.now();
    }
}
