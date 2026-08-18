package com.todayjikgwan.domain.plan;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViewingPlanRepository extends JpaRepository<ViewingPlan, Long> {
    Optional<ViewingPlan> findByUserIdAndSeasonYear(Long userId, int seasonYear);
}
