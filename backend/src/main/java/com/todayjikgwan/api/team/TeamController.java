package com.todayjikgwan.api.team;

import com.todayjikgwan.domain.team.Team;
import com.todayjikgwan.domain.team.TeamRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 응원팀 선택용 구단 목록 */
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamRepository teamRepository;

    @GetMapping
    public List<Map<String, Object>> list() {
        return teamRepository.findAll().stream()
                .filter(Team::isActive)
                .map(t -> Map.<String, Object>of(
                        "id", t.getId(), "name", t.getName(), "shortName", t.getShortName()))
                .toList();
    }
}
