package com.todayjikgwan.domain.game;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRevisionRepository extends JpaRepository<GameRevision, Long> {

    @Query("select r from GameRevision r join fetch r.revisedBy "
            + "where r.game.id = :gameId order by r.revisedAt desc")
    List<GameRevision> findByGame(@Param("gameId") Long gameId);
}
