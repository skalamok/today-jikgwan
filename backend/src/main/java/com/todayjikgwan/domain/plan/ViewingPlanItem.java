package com.todayjikgwan.domain.plan;

import com.todayjikgwan.domain.game.Game;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** REQ-F-403 편성 결과 / REQ-F-404 확정 및 달성률 */
@Getter
@Entity
@Table(name = "viewing_plan_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ViewingPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "viewing_plan_id", nullable = false)
    private ViewingPlan plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanItemStatus status = PlanItemStatus.PROPOSED;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public ViewingPlanItem(ViewingPlan plan, Game game) {
        this.plan = plan;
        this.game = game;
    }

    public void confirm() { this.status = PlanItemStatus.CONFIRMED; }
    public void markDone() { this.status = PlanItemStatus.DONE; }
    public void markMissed() { this.status = PlanItemStatus.MISSED; }
}
