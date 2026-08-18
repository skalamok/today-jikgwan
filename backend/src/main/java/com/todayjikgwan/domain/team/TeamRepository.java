package com.todayjikgwan.domain.team;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByExternalRef(String externalRef);
    List<Team> findByHomeStadiumId(Long stadiumId);
}
