package com.todayjikgwan.domain.badge;

import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    @Query("select ub from UserBadge ub join fetch ub.badge "
            + "where ub.userId = :userId order by ub.achievedAt desc")
    List<UserBadge> findMine(@Param("userId") Long userId);

    @Query("select ub.badge.code from UserBadge ub where ub.userId = :userId")
    Set<String> findCodes(@Param("userId") Long userId);
}
