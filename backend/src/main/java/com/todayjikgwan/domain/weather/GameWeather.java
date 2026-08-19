package com.todayjikgwan.domain.weather;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 경기별 날씨 캐시 (REQ-F-112, REQ-F-113).
 *
 * <p>화면 요청이 기상청 API 로 직접 전달되지 않도록 서버가 배치로 동기화한 값을 보관한다
 * (REQ-NF-016, REQ-NF-024).
 */
@Getter
@Entity
@Table(name = "game_weather")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameWeather {

    @Id
    @Column(name = "game_id")
    private Long gameId;

    @Column(precision = 4, scale = 1)
    private BigDecimal temperature;

    @Column(name = "sky_code", length = 10)
    private String skyCode;

    @Column(name = "precip_type", length = 10)
    private String precipType;

    @Column(name = "precip_prob")
    private Integer precipProb;

    @Column(name = "alert_title", length = 100)
    private String alertTitle;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt = OffsetDateTime.now();

    public GameWeather(Long gameId) {
        this.gameId = gameId;
    }

    public void update(BigDecimal temperature, String skyCode, String precipType, Integer precipProb) {
        this.temperature = temperature;
        this.skyCode = skyCode;
        this.precipType = precipType;
        this.precipProb = precipProb;
        this.fetchedAt = OffsetDateTime.now();
    }

    public void updateAlert(String alertTitle) {
        this.alertTitle = alertTitle;
    }

    /** REQ-F-113 강수 확률이 기준 이상이거나 특보가 발효 중이면 우천 가능성을 안내한다. */
    public boolean isRainRisk(int threshold) {
        if (alertTitle != null && !alertTitle.isBlank()) {
            return true;
        }
        return precipProb != null && precipProb >= threshold;
    }
}
