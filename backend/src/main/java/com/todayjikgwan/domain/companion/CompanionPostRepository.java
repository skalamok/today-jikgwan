package com.todayjikgwan.domain.companion;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanionPostRepository extends JpaRepository<CompanionPost, Long> {

    @Query("select p from CompanionPost p "
            + "join fetch p.game g join fetch g.homeTeam join fetch g.awayTeam join fetch g.stadium "
            + "join fetch p.author "
            + "where :includeClosed = true or p.status = com.todayjikgwan.domain.companion.PostStatus.OPEN "
            + "order by g.startAt")
    List<CompanionPost> findAllForList(@Param("includeClosed") boolean includeClosed);

    @Query("select p from CompanionPost p join fetch p.game g join fetch g.homeTeam "
            + "join fetch g.awayTeam join fetch g.stadium join fetch p.author where p.id = :id")
    Optional<CompanionPost> findDetail(@Param("id") Long id);
}
