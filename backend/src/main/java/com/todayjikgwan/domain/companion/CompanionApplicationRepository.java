package com.todayjikgwan.domain.companion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanionApplicationRepository extends JpaRepository<CompanionApplication, Long> {

    Optional<CompanionApplication> findByCompanionPostIdAndUserId(Long postId, Long userId);

    List<CompanionApplication> findByCompanionPostIdAndStatusOrderBySeq(Long postId, ApplicationStatus status);

    long countByCompanionPostIdAndStatus(Long postId, ApplicationStatus status);

    /** REQ-F-505 같은 시간대의 다른 경기에 이미 확정되어 있는지 검사한다. */
    @Query("select count(a) from CompanionApplication a "
            + "join a.companionPost p join p.game g "
            + "where a.user.id = :userId and a.status = com.todayjikgwan.domain.companion.ApplicationStatus.CONFIRMED "
            + "and g.id <> :gameId and g.startAt between :from and :to")
    long countTimeConflicts(@Param("userId") Long userId,
                            @Param("gameId") Long gameId,
                            @Param("from") OffsetDateTime from,
                            @Param("to") OffsetDateTime to);
}
