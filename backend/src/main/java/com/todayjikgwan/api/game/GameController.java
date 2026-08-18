package com.todayjikgwan.api.game;

import com.todayjikgwan.api.game.dto.WeatherResponse;
import com.todayjikgwan.config.KmaProperties;
import com.todayjikgwan.domain.game.Game;
import com.todayjikgwan.domain.game.GameRepository;
import com.todayjikgwan.domain.weather.GameWeatherRepository;
import com.todayjikgwan.service.weather.WeatherSyncService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameController {

    private final GameRepository gameRepository;
    private final GameWeatherRepository weatherRepository;
    private final WeatherSyncService weatherSyncService;
    private final KmaProperties kmaProperties;

    /** REQ-F-101, REQ-F-102. 날짜 미지정 시 당일 기준. */
    @GetMapping
    public Map<String, Object> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate target = date == null ? LocalDate.now() : date;
        List<Map<String, Object>> content = gameRepository.findByDateWithDetails(target).stream()
                .map(GameController::toMap)
                .toList();

        return Map.of(
                "date", target.toString(),
                "content", content,
                // REQ-N-014 현재는 자체 데이터이므로 외부 출처 표기 대상이 없다
                "dataSource", "자체 등록 데이터");
    }

    /** REQ-F-112 경기 구장 날씨. 동기화된 캐시를 반환하며 외부 API 를 직접 호출하지 않는다 */
    @GetMapping("/{gameId}/weather")
    public WeatherResponse weather(@PathVariable Long gameId) {
        return weatherRepository.findById(gameId)
                .map(w -> WeatherResponse.from(w, kmaProperties.rainRiskThreshold()))
                .orElseGet(WeatherResponse::empty);
    }

    /** 운영 편의용 수동 동기화. 배치(REQ-N-016)와 동일한 로직을 즉시 실행한다 */
    @PostMapping("/weather/sync")
    public Map<String, Object> syncWeather() {
        int updated = weatherSyncService.sync();
        return Map.of("updated", updated, "configured", kmaProperties.isConfigured());
    }

    private static Map<String, Object> toMap(Game g) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", g.getId());
        m.put("gameDate", g.getGameDate().toString());
        m.put("startAt", g.getStartAt().toString());
        m.put("stadium", g.getStadium().getName());
        m.put("homeTeamId", g.getHomeTeam().getId());
        m.put("homeTeam", g.getHomeTeam().getShortName());
        m.put("awayTeamId", g.getAwayTeam().getId());
        m.put("awayTeam", g.getAwayTeam().getShortName());
        m.put("homeScore", g.getHomeScore());
        m.put("awayScore", g.getAwayScore());
        m.put("status", g.getStatus().name());
        m.put("resultConfirmed", g.isResultConfirmed());   // REQ-F-608
        return m;
    }
}
