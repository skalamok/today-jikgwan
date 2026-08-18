package com.todayjikgwan.domain.stat;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStatRepository extends JpaRepository<UserStat, Long> {

    Optional<UserStat> findByUserIdAndDimensionAndDimensionKeyAndSeasonYear(
            Long userId, StatDimension dimension, String dimensionKey, int seasonYear);

    List<UserStat> findByUserIdAndDimensionAndSeasonYear(
            Long userId, StatDimension dimension, int seasonYear);

    List<UserStat> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
