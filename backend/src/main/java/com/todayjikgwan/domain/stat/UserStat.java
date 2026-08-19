package com.todayjikgwan.domain.stat;

import com.todayjikgwan.domain.game.GameResult;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * REQ-NF-003 사전 집계. 매 조회마다 원시 기록을 계산하지 않는다.
 * season_year = 0 은 통산을 의미한다.
 */
@Getter
@Entity
@Table(name = "user_stats")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStat {

    public static final int SEASON_ALL = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatDimension dimension;

    @Column(name = "dimension_key", nullable = false, length = 50)
    private String dimensionKey;

    @Column(name = "season_year", nullable = false)
    private int seasonYear;

    @Column(nullable = false)
    private int games;

    @Column(nullable = false)
    private int wins;

    @Column(nullable = false)
    private int draws;

    @Column(nullable = false)
    private int losses;

    @Column(name = "total_cost", nullable = false)
    private int totalCost;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public UserStat(Long userId, StatDimension dimension, String dimensionKey, int seasonYear) {
        this.userId = userId;
        this.dimension = dimension;
        this.dimensionKey = dimensionKey;
        this.seasonYear = seasonYear;
    }

    public void apply(GameResult result, int cost) {
        if (result == GameResult.NEUTRAL) {
            this.totalCost += cost;
            this.updatedAt = OffsetDateTime.now();
            return;
        }
        this.games++;
        switch (result) {
            case WIN -> this.wins++;
            case DRAW -> this.draws++;
            case LOSE -> this.losses++;
            default -> { }
        }
        this.totalCost += cost;
        this.updatedAt = OffsetDateTime.now();
    }

    public void reset() {
        this.games = 0; this.wins = 0; this.draws = 0; this.losses = 0; this.totalCost = 0;
        this.updatedAt = OffsetDateTime.now();
    }
}
