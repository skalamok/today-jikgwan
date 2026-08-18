package com.todayjikgwan.api.stadium.dto;

import java.util.List;

public record StadiumDetailResponse(
        Long id,
        String name,
        String nameEn,
        Integer capacity,
        List<String> homeTeams,
        List<ZoneStatResponse> zones,
        MyRecord myRecord) {

    /** 로그인 사용자의 해당 구장 전적. 비로그인이거나 기록이 없으면 null. */
    public record MyRecord(int games, int wins, int draws, int losses) { }
}
