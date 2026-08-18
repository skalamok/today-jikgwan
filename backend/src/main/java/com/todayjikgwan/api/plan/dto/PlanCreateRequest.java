package com.todayjikgwan.api.plan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** REQ-F-401, REQ-F-402 */
public record PlanCreateRequest(
        @NotNull Integer seasonYear,
        @NotNull @Min(1) @Max(144) Integer targetCount,
        @Min(0) Integer budgetTotal,
        @Min(0) Integer maxCostPerGame,
        List<String> availableDays,       // MONDAY, SATURDAY ...
        List<Long> stadiumIds,            // 이동 가능 구장. 비우면 전 구장
        @Min(0) @Max(100) Integer maxPrecipProb) {
}
