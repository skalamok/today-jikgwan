package com.todayjikgwan.api.game;

import com.todayjikgwan.api.game.dto.StandingResponse;
import com.todayjikgwan.service.StandingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REQ-F-104. API 명세의 /api/v1/standings 와 경로를 맞추기 위해 별도 컨트롤러로 둔다 */
@RestController
@RequestMapping("/api/v1/standings")
@RequiredArgsConstructor
public class StandingController {

    private final StandingService standingService;

    @GetMapping
    public List<StandingResponse> standings(@RequestParam(defaultValue = "2026") int season) {
        return standingService.standings(season);
    }
}
