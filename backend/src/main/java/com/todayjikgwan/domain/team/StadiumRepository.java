package com.todayjikgwan.domain.team;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StadiumRepository extends JpaRepository<Stadium, Long> {
    java.util.Optional<Stadium> findByName(String name);
}
