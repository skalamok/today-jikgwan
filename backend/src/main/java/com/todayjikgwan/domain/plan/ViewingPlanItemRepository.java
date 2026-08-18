package com.todayjikgwan.domain.plan;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ViewingPlanItemRepository extends JpaRepository<ViewingPlanItem, Long> {

    @Query("select i from ViewingPlanItem i join fetch i.game g "
            + "join fetch g.homeTeam join fetch g.awayTeam join fetch g.stadium "
            + "where i.plan.id = :planId order by g.startAt")
    List<ViewingPlanItem> findByPlanWithGame(@Param("planId") Long planId);

    void deleteByPlanId(Long planId);
}
