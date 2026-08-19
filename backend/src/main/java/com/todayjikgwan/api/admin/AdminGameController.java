package com.todayjikgwan.api.admin;

import com.todayjikgwan.domain.game.GameStatus;
import com.todayjikgwan.security.CurrentUser;
import com.todayjikgwan.service.AdminGameService;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REQ-F-601 ~ 604, REQ-F-607. 경로 전체가 ROLE_ADMIN 이다 (SecurityConfig, REQ-N-009) */
@RestController
@RequestMapping("/api/v1/admin/games")
@RequiredArgsConstructor
public class AdminGameController {

    private final AdminGameService adminGameService;

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
}
