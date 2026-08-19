package com.todayjikgwan.api.attendance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** REQ-F-201 ~ 211 */
public record AttendanceLogRequest(
        @NotNull Long gameId,
        Long cheerTeamId,                                  // null 이면 중립 관람
        @NotNull Long stadiumZoneId,
        @NotNull @Min(1) @Max(5) Short zoneRating,         // REQ-F-206 필수
        @Size(max = 1000) String memo,
        @Min(1) @Max(5) Short gameRating,
        @Min(0) Integer ticketCost,
        @Min(0) Integer foodCost,
        @Min(0) Integer transportCost,
        String visibility,
        /** REQ-F-209 함께 간 사람. 회원이면 userId, 아니면 name 만 채운다 */
        java.util.List<Companion> companions) {

    public record Companion(Long userId, @Size(max = 30) String name) { }
}
