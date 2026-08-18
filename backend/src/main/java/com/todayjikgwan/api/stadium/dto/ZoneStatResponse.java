package com.todayjikgwan.api.stadium.dto;

/**
 * REQ-F-110 구역별 만족도.
 * 표본이 기준 미만이면 avgRating 을 내리지 않고 smallSample=true 로 표시한다 (REQ-F-305).
 */
public record ZoneStatResponse(
        Long zoneId,
        String name,
        Double avgRating,
        int ratingCount,
        boolean smallSample) {
}
