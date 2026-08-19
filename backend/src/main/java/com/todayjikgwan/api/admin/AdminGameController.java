package com.todayjikgwan.api.admin;

import com.todayjikgwan.domain.game.GameStatus;
import com.todayjikgwan.security.CurrentUser;
import com.todayjikgwan.service.AdminGameService;
import com.todayjikgwan.service.gamedata.GameSyncService;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REQ-F-601 ~ 606. 경로 전체가 ROLE_ADMIN 이다 (SecurityConfig, REQ-NF-009) */
@RestController
@RequestMapping("/api/v1/admin/games")
@RequiredArgsConstructor
public class AdminGameController {

    private final AdminGameService adminGameService;
    private final GameSyncService gameSyncService;

    @PostMapping
    public ResponseEntity<Map<String, Long>> register(@RequestBody RegisterRequest request) {
        Long id = adminGameService.register(request.seasonYear(), request.startAt(),
                request.stadiumId(), request.homeTeamId(), request.awayTeamId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("gameId", id));
    }

    @GetMapping("/unconfirmed")
    public List<Map<String, Object>> unconfirmed() {
        return adminGameService.unconfirmed();
    }

    @PostMapping("/{gameId}/revisions")
    public Map<String, Object> revise(@PathVariable Long gameId,
                                      @RequestBody ReviseRequest request) {
        return adminGameService.revise(gameId, CurrentUser.id(),
                request.status(), request.homeScore(), request.awayScore(), request.reason());
    }

    @GetMapping("/{gameId}/revisions")
    public List<Map<String, Object>> revisions(@PathVariable Long gameId) {
        return adminGameService.revisions(gameId);
    }

    public record RegisterRequest(
            @NotNull Integer seasonYear,
            @NotNull OffsetDateTime startAt,
            @NotNull Long stadiumId,
            @NotNull Long homeTeamId,
            @NotNull Long awayTeamId) { }

    public record ReviseRequest(
            @NotNull GameStatus status,
            Integer homeScore,
            Integer awayScore,
            String reason) { }

    /**
     * REQ-F-107 경기 데이터 동기화.
     *
     * 붙어 있는 제공자를 모두 물어본다. 켜진 외부 제공자가 없으면 아무 일도 일어나지
     * 않으며 그것이 기본 상태다. 운영자가 확정한 결과는 덮어쓰지 않는다 (REQ-NF-015).
     */
    @PostMapping("/sync")
    public Map<String, Object> sync(
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(
                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate date) {
        return gameSyncService.sync(date == null ? java.time.LocalDate.now() : date);
    }
}
