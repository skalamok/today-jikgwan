package com.todayjikgwan.api.game;

import com.todayjikgwan.api.game.dto.WeatherResponse;
import com.todayjikgwan.config.KmaProperties;
import com.todayjikgwan.domain.game.Game;
import com.todayjikgwan.domain.game.GameSource;
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
                // REQ-NF-014 현재는 자체 데이터이므로 외부 출처 표기 대상이 없다
                "dataSource", "자체 등록 데이터");
    }

    /** REQ-F-103 경기 상세. 목록과 같은 형태를 단건으로 준다 */
    @GetMapping("/{gameId}")
    public Map<String, Object> detail(@PathVariable Long gameId) {
        return gameRepository.findDetailById(gameId)
                .map(GameController::toMap)
                .orElseThrow(() -> new com.todayjikgwan.common.exception.ApiException(
                        com.todayjikgwan.common.exception.ErrorCode.NOT_FOUND));
    }

    /**
     * REQ-F-204 촬영 일시로 그날 경기를 골라 준다.
     *
     * <p>사진의 촬영 시각은 현지 시간이므로 그 날짜의 경기를 모두 준다. 한 날에 다섯
     * 경기가 열리므로 하나로 좁히지 않고 후보로 돌려주고 고르는 것은 사람이 한다.
     * 자정 넘어 끝난 연장전을 생각해 앞날 경기도 함께 본다.
     *
     * <p>촬영 시각이 없거나 그날 경기가 없으면 빈 배열이다. 화면은 수동 선택으로 넘어간다.
     */
    @PostMapping("/suggest")
    public List<Map<String, Object>> suggest(@RequestBody Map<String, String> body) {
        String takenAt = body.get("takenAt");
        if (takenAt == null || takenAt.isBlank()) {
            return List.of();
        }
        LocalDate date;
        try {
            date = LocalDate.parse(takenAt.substring(0, 10));
        } catch (Exception e) {
            // 읽을 수 없는 값이면 후보를 못 낼 뿐이다. 오류로 막으면 사진을 못 올린다
            return List.of();
        }
        List<Map<String, Object>> out = new java.util.ArrayList<>(
                gameRepository.findByDateWithDetails(date).stream().map(GameController::toMap).toList());
        // 밤 경기가 자정을 넘겨 끝나면 사진의 날짜가 하루 뒤가 된다
        gameRepository.findByDateWithDetails(date.minusDays(1)).stream()
                .map(GameController::toMap)
                .forEach(out::add);
        return out;
    }

    /** REQ-F-112 경기 구장 날씨. 동기화된 캐시를 반환하며 외부 API 를 직접 호출하지 않는다 */
    @GetMapping("/{gameId}/weather")
    public WeatherResponse weather(@PathVariable Long gameId) {
        return weatherRepository.findById(gameId)
                .map(w -> WeatherResponse.from(w, kmaProperties.rainRiskThreshold()))
                .orElseGet(WeatherResponse::empty);
    }

    /** 운영 편의용 수동 동기화. 배치(REQ-NF-016)와 동일한 로직을 즉시 실행한다 */
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
        m.put("resultConfirmed", g.isResultConfirmed());   // REQ-F-606
        // REQ-F-106 외부에서 받아 온 값에는 마지막 동기화 시각과 출처를 함께 붙인다.
        // 운영자가 넣은 것은 우리 데이터라 표기할 출처가 없다 (REQ-NF-014)
        if (g.getSource() != GameSource.MANUAL && g.getSyncedAt() != null) {
            m.put("dataSource", g.getSource().name());
            m.put("syncedAt", g.getSyncedAt().toString());
        }
        return m;
    }
}
