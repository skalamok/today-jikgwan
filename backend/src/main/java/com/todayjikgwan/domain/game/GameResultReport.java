package com.todayjikgwan.domain.game;

import com.todayjikgwan.domain.user.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * REQ-F-606, REQ-F-607 사용자 스코어 제보.
 * 관람 기록 작성자만 제보할 수 있으므로 일반 제보보다 신뢰도가 높다.
 */
@Getter
@Entity
@Table(name = "game_result_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameResultReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "home_score", nullable = false)
    private int homeScore;

    @Column(name = "away_score", nullable = false)
    private int awayScore;

    @Column(name = "reported_at", nullable = false)
    private OffsetDateTime reportedAt = OffsetDateTime.now();

    public GameResultReport(Game game, User user, int homeScore, int awayScore) {
        this.game = game;
        this.user = user;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }
}
