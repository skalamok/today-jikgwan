package com.todayjikgwan.domain.game;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRepository extends JpaRepository<Game, Long> {

    @Query("select g from Game g "
        + "join fetch g.homeTeam join fetch g.awayTeam join fetch g.stadium "
        + "where g.gameDate = :date order by g.startAt")
    List<Game> findByDateWithDetails(@Param("date") LocalDate date);

    /** REQ-F-112 단기예보 제공 범위(3일) 내의 예정 경기 */
    @Query("select g from Game g join fetch g.stadium "
            + "where g.gameDate between :from and :to order by g.startAt")
    List<Game> findUpcomingBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** REQ-F-403 편성 대상. 시즌 내 아직 열리지 않은 경기 */
    @Query("select g from Game g join fetch g.stadium join fetch g.homeTeam join fetch g.awayTeam "
            + "where g.seasonYear = :season and g.gameDate >= :from "
            + "and g.status = com.todayjikgwan.domain.game.GameStatus.SCHEDULED "
            + "order by g.startAt")
    List<Game> findUpcomingInSeason(@Param("season") int season, @Param("from") LocalDate from);
}
