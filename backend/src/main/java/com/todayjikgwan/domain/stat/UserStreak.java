package com.todayjikgwan.domain.stat;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** REQ-F-306. 무승부는 연승을 끊지 않고 유지한다. */
@Getter
@Entity
@Table(name = "user_streaks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStreak {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "longest_win", nullable = false)
    private int longestWin;

    @Column(name = "last_game_date")
    private LocalDate lastGameDate;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public UserStreak(Long userId) {
        this.userId = userId;
    }

    public void update(int currentStreak, int longestWin, LocalDate lastGameDate) {
        this.currentStreak = currentStreak;
        this.longestWin = longestWin;
        this.lastGameDate = lastGameDate;
        this.updatedAt = OffsetDateTime.now();
    }
}
