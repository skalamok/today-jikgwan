package com.todayjikgwan.api.attendance.dto;

import com.todayjikgwan.domain.attendance.AttendanceLog;
import com.todayjikgwan.domain.game.Game;
import java.time.LocalDate;

public record AttendanceLogResponse(
        Long id,
        LocalDate gameDate,
        String stadiumName,
        String matchup,
        String result,
        String zoneName,
        Short zoneRating,
        String memo,
        Integer totalCost,
        boolean resultConfirmed) {

    public static AttendanceLogResponse from(AttendanceLog log) {
        Game g = log.getGame();
        String matchup = g.isResultConfirmed()
                ? "%s %d : %d %s".formatted(g.getHomeTeam().getShortName(), g.getHomeScore(),
                                            g.getAwayScore(), g.getAwayTeam().getShortName())
                : "%s vs %s".formatted(g.getHomeTeam().getShortName(), g.getAwayTeam().getShortName());
        return new AttendanceLogResponse(
                log.getId(), g.getGameDate(), g.getStadium().getName(), matchup,
                log.result().name(), log.getStadiumZone().getName(), log.getZoneRating(),
                log.getMemo(), log.totalCost(), g.isResultConfirmed());
    }
}
