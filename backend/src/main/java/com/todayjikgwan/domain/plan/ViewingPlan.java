package com.todayjikgwan.domain.plan;

import com.todayjikgwan.domain.user.User;
import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시즌 관람 계획 (REQ-F-401, REQ-F-402).
 *
 * <p>사용자가 입력한 제약 조건을 보관한다. 편성 결과는 {@link ViewingPlanItem} 이다.
 */
@Getter
@Entity
@Table(name = "viewing_plans")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ViewingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "season_year", nullable = false)
    private int seasonYear;

    /** 이번 시즌에 가고 싶은 경기 수 */
    @Column(name = "target_count", nullable = false)
    private int targetCount;

    @Column(name = "budget_total")
    private Integer budgetTotal;

    @Column(name = "max_cost_per_game")
    private Integer maxCostPerGame;

    /** 관람 가능 요일. "MONDAY,SATURDAY" 형태로 보관한다 */
    @Column(name = "available_days", length = 30)
    private String availableDays;

    /** REQ-F-402 허용 강수 확률(%). 이 값을 초과하는 경기는 제외한다 */
    @Column(name = "max_precip_prob")
    private Integer maxPrecipProb;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public ViewingPlan(User user, int seasonYear, int targetCount, Integer budgetTotal,
                       Integer maxCostPerGame, Set<DayOfWeek> days, Integer maxPrecipProb) {
        this.user = user;
        this.seasonYear = seasonYear;
        this.targetCount = targetCount;
        this.budgetTotal = budgetTotal;
        this.maxCostPerGame = maxCostPerGame;
        this.availableDays = (days == null || days.isEmpty()) ? null
                : days.stream().map(Enum::name).reduce((a, b) -> a + "," + b).orElse(null);
        this.maxPrecipProb = maxPrecipProb;
    }

    public Set<DayOfWeek> days() {
        if (availableDays == null || availableDays.isBlank()) {
            return EnumSet.allOf(DayOfWeek.class);      // 미지정이면 제약 없음
        }
        return Arrays.stream(availableDays.split(","))
                .map(String::trim).map(DayOfWeek::valueOf)
                .collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }
}
