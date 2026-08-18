package com.todayjikgwan.api.stat.dto;

public record StatItemResponse(
        String key,
        String label,
        int games,
        int wins,
        int draws,
        int losses,
        Double winRate,
        boolean smallSample) {
}
