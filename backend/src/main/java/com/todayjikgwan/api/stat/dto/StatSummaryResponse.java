package com.todayjikgwan.api.stat.dto;

/**
 * REQ-F-305 소표본 표시 정책.
 * 표본이 기준 미만이면 winRate 를 null 로 내리고 smallSample=true 를 함께 보낸다.
 * 클라이언트는 승률 대신 전적만 표시한다. (2경기 2승을 승률 1.000 으로 보여주지 않기 위함)
 */
public record StatSummaryResponse(
        Integer seasonYear,
        int games,
        int wins,
        int draws,
        int losses,
        Double winRate,
        boolean smallSample,
        int neutralCount,
        int currentStreak,
        int longestWinStreak,
        int totalCost) {
}
