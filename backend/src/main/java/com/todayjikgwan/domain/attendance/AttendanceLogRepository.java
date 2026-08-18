package com.todayjikgwan.domain.attendance;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

    boolean existsByUserIdAndGameIdAndDeletedAtIsNull(Long userId, Long gameId);

    Optional<AttendanceLog> findByUserIdAndGameId(Long userId, Long gameId);

    @Query("select l from AttendanceLog l "
            + "join fetch l.game g join fetch g.homeTeam join fetch g.awayTeam join fetch g.stadium "
            + "where l.user.id = :userId and l.deletedAt is null "
            + "order by g.gameDate desc")
    List<AttendanceLog> findMine(@Param("userId") Long userId);

    /** REQ-F-111 구역별 공개 후기 */
    @Query("select l from AttendanceLog l join fetch l.game g "
            + "where l.stadiumZone.id = :zoneId and l.deletedAt is null "
            + "and l.visibility = com.todayjikgwan.domain.attendance.Visibility.PUBLIC "
            + "and l.memo is not null order by g.gameDate desc")
    List<AttendanceLog> findPublicReviewsByZone(@Param("zoneId") Long zoneId, Pageable pageable);
}
