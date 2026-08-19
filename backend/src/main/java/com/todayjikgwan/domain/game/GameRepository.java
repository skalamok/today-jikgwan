package com.todayjikgwan.domain.game;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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

    /** REQ-F-103 단건 조회. 목록과 같은 값을 채우려면 연관을 함께 가져와야 한다 */
    @Query("select g from Game g "
            + "join fetch g.homeTeam join fetch g.awayTeam join fetch g.stadium "
            + "where g.id = :id")
    java.util.Optional<Game> findDetailById(@Param("id") Long id);

    /** REQ-F-601 운영자 검토 대상. 이미 시작했는데 아직 결과가 확정되지 않은 경기 */
    @Query("select g from Game g "
            + "join fetch g.homeTeam join fetch g.awayTeam join fetch g.stadium "
            + "where g.resultConfirmed = false and g.startAt < :now "
            + "and g.status <> com.todayjikgwan.domain.game.GameStatus.CANCELED "
            + "order by g.startAt desc")
    List<Game> findUnconfirmedBefore(@Param("now") OffsetDateTime now);

    /** REQ-F-601 같은 날 같은 구장에 이미 등록된 경기가 있는지 본다 */
    boolean existsByGameDateAndStadiumIdAndHomeTeamIdAndAwayTeamId(
            LocalDate gameDate, Long stadiumId, Long homeTeamId, Long awayTeamId);

    /** REQ-F-104 순위 산출용. 결과가 확정된 경기만 */
    @Query("select g from Game g join fetch g.homeTeam join fetch g.awayTeam "
            + "where g.seasonYear = :season and g.resultConfirmed = true "
            + "and g.homeScore is not null and g.awayScore is not null")
    List<Game> findFinishedInSeason(@Param("season") int season);
}
