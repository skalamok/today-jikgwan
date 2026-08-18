package com.todayjikgwan.api.game.dto;

/** REQ-F-104. 외부 순위표를 가져오지 않고 등록된 경기 결과로부터 직접 산출한다 */
public record StandingResponse(
        int rank, Long teamId, String team, int games,
        int wins, int draws, int losses, Double winRate, Double gamesBehind) {
}
