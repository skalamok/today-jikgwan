package com.todayjikgwan.domain.game;

import com.todayjikgwan.domain.user.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * REQ-F-603 경기 결과 정정 이력.
 *
 * <p>개인 전적을 바꾸는 변경이라 추적할 수 있어야 한다. <b>추가 전용</b>으로,
 * 한 번 남긴 행은 수정하거나 지우지 않는다. 그래서 수정자(setter)를 두지 않는다.
 */
@Getter
@Entity
@Table(name = "game_revisions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(name = "before_status", length = 20)
    private GameStatus beforeStatus;

    @Column(name = "before_home_score")
    private Integer beforeHomeScore;

    @Column(name = "before_away_score")
    private Integer beforeAwayScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "after_status", nullable = false, length = 20)
    private GameStatus afterStatus;

    @Column(name = "after_home_score")
    private Integer afterHomeScore;

    @Column(name = "after_away_score")
    private Integer afterAwayScore;

    @Column(nullable = false, length = 255)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revised_by", nullable = false)
    private User revisedBy;

    @Column(name = "revised_at", nullable = false)
    private OffsetDateTime revisedAt = OffsetDateTime.now();

    /** 변경 전 값은 호출 시점의 경기 상태에서 그대로 떠 둔다. */
    public static GameRevision of(Game game, GameStatus beforeStatus,
                                  Integer beforeHome, Integer beforeAway,
                                  String reason, User revisedBy) {
        GameRevision r = new GameRevision();
        r.game = game;
        r.beforeStatus = beforeStatus;
        r.beforeHomeScore = beforeHome;
        r.beforeAwayScore = beforeAway;
        r.afterStatus = game.getStatus();
        r.afterHomeScore = game.getHomeScore();
        r.afterAwayScore = game.getAwayScore();
        r.reason = reason;
        r.revisedBy = revisedBy;
        r.revisedAt = OffsetDateTime.now();
        return r;
    }
}
