package com.todayjikgwan.domain.game;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameResultReportRepository extends JpaRepository<GameResultReport, Long> {

    boolean existsByGameIdAndUserId(Long gameId, Long userId);

    /** REQ-F-607 동일 스코어 제보를 집계한다. 최다 일치 건이 앞에 온다. */
    @Query("select r.homeScore as homeScore, r.awayScore as awayScore, count(r) as cnt "
        + "from GameResultReport r where r.game.id = :gameId "
        + "group by r.homeScore, r.awayScore order by count(r) desc")
    List<ReportTally> tally(@Param("gameId") Long gameId);

    interface ReportTally {
        int getHomeScore();
        int getAwayScore();
        long getCnt();
    }
}
