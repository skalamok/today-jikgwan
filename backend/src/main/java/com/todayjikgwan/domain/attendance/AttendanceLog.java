package com.todayjikgwan.domain.attendance;

import com.todayjikgwan.common.entity.BaseTimeEntity;
import com.todayjikgwan.domain.game.Game;
import com.todayjikgwan.domain.game.GameResult;
import com.todayjikgwan.domain.team.StadiumZone;
import com.todayjikgwan.domain.team.Team;
import com.todayjikgwan.domain.user.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관람 기록 1건. 이 서비스의 원자 단위이자 전적·구역만족도 집계의 원천 데이터다.
 * (user_id, game_id) 유니크 제약으로 동일 경기 중복 기록을 DB에서 차단한다 (REQ-F-201).
 */
@Getter
@Entity
@Table(name = "attendance_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    /**
     * 기록 시점의 응원팀. null 이면 중립 관람이다 (REQ-F-202).
     * 사용자가 프로필의 응원팀을 바꿔도 이 값은 유지되므로 과거 판정이 뒤집히지 않는다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cheer_team_id")
    private Team cheerTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stadium_zone_id", nullable = false)
    private StadiumZone stadiumZone;

    /** REQ-F-206 필수. 구장 상세의 구역별 만족도 집계 입력원이므로 생략을 허용하지 않는다. */
    @Column(name = "zone_rating", nullable = false)
    private Short zoneRating;

    @Column(length = 1000)
    private String memo;

    @Column(name = "game_rating")
    private Short gameRating;

    @Column(name = "ticket_cost")
    private Integer ticketCost;

    @Column(name = "food_cost")
    private Integer foodCost;

    @Column(name = "transport_cost")
    private Integer transportCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Visibility visibility = Visibility.PRIVATE;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @OneToMany(mappedBy = "attendanceLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttendancePhoto> photos = new ArrayList<>();

    @Builder
    private AttendanceLog(User user, Game game, Team cheerTeam, StadiumZone stadiumZone,
                          Short zoneRating, String memo, Short gameRating,
                          Integer ticketCost, Integer foodCost, Integer transportCost,
                          Visibility visibility) {
        this.user = user;
        this.game = game;
        this.cheerTeam = cheerTeam;
        this.stadiumZone = stadiumZone;
        this.zoneRating = zoneRating;
        this.memo = memo;
        this.gameRating = gameRating;
        this.ticketCost = ticketCost;
        this.foodCost = foodCost;
        this.transportCost = transportCost;
        this.visibility = visibility == null ? Visibility.PRIVATE : visibility;
    }

    public Long cheerTeamId() {
        return cheerTeam == null ? null : cheerTeam.getId();
    }

    public GameResult result() {
        return game.resultFor(cheerTeamId());
    }

    public int totalCost() {
        return nz(ticketCost) + nz(foodCost) + nz(transportCost);
    }

    private int nz(Integer v) { return v == null ? 0 : v; }
}
