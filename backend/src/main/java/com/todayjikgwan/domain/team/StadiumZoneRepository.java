package com.todayjikgwan.domain.team;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StadiumZoneRepository extends JpaRepository<StadiumZone, Long> {
    List<StadiumZone> findByStadiumIdOrderBySortOrder(Long stadiumId);
}
