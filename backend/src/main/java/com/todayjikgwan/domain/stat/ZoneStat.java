package com.todayjikgwan.domain.stat;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * REQ-F-110 구역별 만족도.
 * 평균 대신 합계와 개수를 저장한다. 새 평가는 더하기만 하면 되고,
 * ratingCount 가 소표본 판정(REQ-F-305)에 그대로 쓰인다.
 */
@Getter
@Entity
@Table(name = "zone_stats")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ZoneStat {

    @Id
    @Column(name = "stadium_zone_id")
    private Long stadiumZoneId;

    @Column(name = "rating_sum", nullable = false)
    private int ratingSum;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public void addRating(int rating) {
        this.ratingSum += rating;
        this.ratingCount++;
        recalc();
    }

    public void removeRating(int rating) {
        this.ratingSum -= rating;
        this.ratingCount--;
        recalc();
    }

    private void recalc() {
        this.avgRating = ratingCount == 0 ? null
                : BigDecimal.valueOf(ratingSum)
                        .divide(BigDecimal.valueOf(ratingCount), 2, RoundingMode.HALF_UP);
        this.updatedAt = OffsetDateTime.now();
    }
}
