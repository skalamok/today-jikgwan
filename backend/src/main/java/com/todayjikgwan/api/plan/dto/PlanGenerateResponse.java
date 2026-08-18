package com.todayjikgwan.api.plan.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * REQ-F-403 편성 결과.
 *
 * <p>목표를 채우지 못한 경우 그냥 "조건에 맞는 경기가 없습니다"로 끝내지 않고,
 * <b>어떤 제약을 완화하면 몇 경기를 더 확보할 수 있는지</b>를 함께 돌려준다.
 */
public record PlanGenerateResponse(
        int targetCount,
        int fulfilledCount,
        Integer estimatedCost,
        List<ProposedGame> proposed,
        List<FilterStat> filteredOut,
        List<Relaxation> unsatisfied) {

    public record ProposedGame(
            Long gameId, LocalDate gameDate, String dayOfWeek, String startTime,
            String stadium, String matchup, boolean cheerTeamGame,
            Integer precipProbability, Integer estimatedCost) { }

    /** 제약별로 몇 경기가 걸러졌는지. 편성 결과의 근거를 보여준다 */
    public record FilterStat(String constraint, String label, int excluded) { }

    /** 목표 미달 시 완화 후보 */
    public record Relaxation(String constraint, String message, int additionalGames) { }
}
