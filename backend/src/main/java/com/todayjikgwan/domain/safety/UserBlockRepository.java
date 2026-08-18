package com.todayjikgwan.domain.safety;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    List<UserBlock> findByUserId(Long userId);
    boolean existsByUserIdAndBlockedId(Long userId, Long blockedId);
    void deleteByUserIdAndBlockedId(Long userId, Long blockedId);
}
