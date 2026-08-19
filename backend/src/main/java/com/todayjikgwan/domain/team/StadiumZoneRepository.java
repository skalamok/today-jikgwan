package com.todayjikgwan.domain.team;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StadiumZoneRepository extends JpaRepository<StadiumZone, Long> {
    /** 운영자용. 비활성 구역도 포함한다 (되살릴 수 있어야 한다) */
    List<StadiumZone> findByStadiumIdOrderBySortOrder(Long stadiumId);

    /** 일반 조회용. 비활성 구역은 새 기록에서 고를 수 없어야 하므로 뺀다 (REQ-F-605) */
    List<StadiumZone> findByStadiumIdAndActiveTrueOrderBySortOrder(Long stadiumId);

    boolean existsByStadiumIdAndName(Long stadiumId, String name);
}
