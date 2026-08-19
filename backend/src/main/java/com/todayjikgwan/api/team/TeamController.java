package com.todayjikgwan.api.team;

import com.todayjikgwan.api.team.dto.TeamResponse;
import com.todayjikgwan.domain.team.Team;
import com.todayjikgwan.domain.team.TeamRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 응원팀 선택용 구단 목록 */
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamRepository teamRepository;

    /** 응원팀 선택과 구단 공식 채널(REQ-F-114)에 함께 쓴다 */
    @GetMapping
    public List<TeamResponse> list() {
        return teamRepository.findAll().stream()
                .filter(Team::isActive)
                .map(TeamResponse::from)
                .toList();
    }
}
