package com.todayjikgwan.domain.companion;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionMessageReadRepository extends JpaRepository<CompanionMessageRead, Long> {
    Optional<CompanionMessageRead> findByCompanionPostIdAndUserId(Long postId, Long userId);
}
