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

    /** REQ-F-606 미확정 경기는 화면에서 "확인 중"으로 구분 표시한다 */
    @Column(name = "result_confirmed", nullable = false)
    private boolean resultConfirmed = false;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    /**
     * REQ-F-601 운영자 등록. 외부 소스를 확보하지 못했을 때의 기본 수단이라
     * 결과 없이 일정만 먼저 넣을 수 있어야 한다.
     */
    public static Game schedule(int seasonYear, OffsetDateTime startAt, Stadium stadium,
                                Team homeTeam, Team awayTeam) {
        Game g = new Game();
        g.seasonYear = seasonYear;
        g.startAt = startAt;
        g.gameDate = startAt.toLocalDate();
        g.stadium = stadium;
        g.homeTeam = homeTeam;
        g.awayTeam = awayTeam;
        g.status = GameStatus.SCHEDULED;
        g.source = GameSource.MANUAL;
        return g;
    }

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

    /**
     * REQ-F-602 운영자 정정. 제보 확정과 달리 상태만 바꾸는 경우(우천 취소 등)도 있어
     * 스코어를 비우는 것까지 허용한다.
     */
    public void revise(GameStatus status, Integer home, Integer away) {
        this.status = status;
        this.homeScore = home;
        this.awayScore = away;
        this.source = GameSource.MANUAL;
        // 스코어가 있는 종료 경기만 확정으로 본다. 취소·서스펜디드는 전적에서 빠져야 한다.
        this.resultConfirmed = status == GameStatus.FINISHED && home != null && away != null;
        this.confirmedAt = this.resultConfirmed ? OffsetDateTime.now() : null;
    }
}
