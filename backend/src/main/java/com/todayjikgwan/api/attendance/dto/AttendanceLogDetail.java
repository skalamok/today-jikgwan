package com.todayjikgwan.api.attendance.dto;

import com.todayjikgwan.domain.attendance.AttendanceLog;
import com.todayjikgwan.domain.attendance.AttendancePhoto;
import com.todayjikgwan.domain.game.Game;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** REQ-F-214 기록 상세 */
public record AttendanceLogDetail(
        Long id, Long gameId, LocalDate gameDate, String stadiumName, String zoneName,
        String matchup, String result, String cheerTeam,
        Short zoneRating, Short gameRating, String memo, String visibility,
        Integer ticketCost, Integer foodCost, Integer transportCost, Integer totalCost,
        String weatherSky, Double weatherTemp,
        List<Photo> photos, OffsetDateTime createdAt) {

    public record Photo(Long id, String originalUrl, String thumbnailUrl, OffsetDateTime takenAt) { }

    public static AttendanceLogDetail from(AttendanceLog l, List<AttendancePhoto> photos) {
        Game g = l.getGame();
        String matchup = g.isResultConfirmed()
                ? "%s %d : %d %s".formatted(g.getHomeTeam().getShortName(), g.getHomeScore(),
                                            g.getAwayScore(), g.getAwayTeam().getShortName())
                : "%s vs %s".formatted(g.getHomeTeam().getShortName(), g.getAwayTeam().getShortName());
        return new AttendanceLogDetail(
                l.getId(), g.getId(), g.getGameDate(), g.getStadium().getName(),
                l.getStadiumZone().getName(), matchup, l.result().name(),
                l.getCheerTeam() == null ? null : l.getCheerTeam().getName(),
                l.getZoneRating(), l.getGameRating(), l.getMemo(), l.getVisibility().name(),
                l.getTicketCost(), l.getFoodCost(), l.getTransportCost(), l.totalCost(),
                l.getWeatherSky(), l.getWeatherTemp() == null ? null : l.getWeatherTemp().doubleValue(),
                photos.stream().map(p -> new Photo(p.getId(), p.getOriginalUrl(),
                        p.getThumbnailUrl(), p.getTakenAt())).toList(),
                l.getCreatedAt());
    }
}
