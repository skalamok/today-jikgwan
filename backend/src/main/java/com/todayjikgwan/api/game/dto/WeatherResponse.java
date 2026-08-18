package com.todayjikgwan.api.game.dto;

import com.todayjikgwan.domain.weather.GameWeather;
import java.time.OffsetDateTime;

/** REQ-F-112, REQ-F-113. REQ-N-024 에 따라 출처를 함께 반환한다. */
public record WeatherResponse(
        Double temperature,
        String sky,
        String precipType,
        Integer precipProbability,
        String alert,
        boolean rainRisk,
        OffsetDateTime fetchedAt,
        String dataSource) {

    private static final String SOURCE = "기상청 단기예보 (공공데이터포털, 공공누리 출처표시)";

    public static WeatherResponse from(GameWeather w, int threshold) {
        return new WeatherResponse(
                w.getTemperature() == null ? null : w.getTemperature().doubleValue(),
                w.getSkyCode(), w.getPrecipType(), w.getPrecipProb(),
                w.getAlertTitle(), w.isRainRisk(threshold), w.getFetchedAt(), SOURCE);
    }

    /** 예보 범위를 벗어났거나 아직 동기화되지 않은 경우 */
    public static WeatherResponse empty() {
        return new WeatherResponse(null, null, null, null, null, false, null, SOURCE);
    }
}
