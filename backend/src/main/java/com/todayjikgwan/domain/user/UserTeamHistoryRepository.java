package com.todayjikgwan.domain.user;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** REQ-F-005 응원팀 변경 이력 */
public interface UserTeamHistoryRepository extends JpaRepository<UserTeamHistory, Long> {

    List<UserTeamHistory> findByUserIdOrderByChangedAtDesc(Long userId);
}
