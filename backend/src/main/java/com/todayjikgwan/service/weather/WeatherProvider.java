package com.todayjikgwan.service.weather;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 날씨 제공자 추상화.
 *
 * <p>경기 데이터(REQ-F-107)와 같은 이유로 인터페이스를 둔다.
 * 공공데이터포털과 기상청 API허브 중 어느 쪽을 쓰든 구현체 교체만으로 전환할 수 있다.
 */
public interface WeatherProvider {

    /** 특정 격자의 지정 시각 예보. 예보 범위를 벗어나면 empty. */
    Optional<Forecast> forecast(int nx, int ny, OffsetDateTime target);

    /** 지역 기상특보. 미발효 시 empty. */
    Optional<String> alert(String regionCode);

    record Forecast(BigDecimal temperature, String sky, String precipType, Integer precipProbability) { }
}
