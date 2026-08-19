package com.todayjikgwan.service;

import com.todayjikgwan.common.exception.ApiException;
import com.todayjikgwan.common.exception.ErrorCode;
import com.todayjikgwan.config.TodayJikgwanProperties;
import com.todayjikgwan.domain.attendance.AttendanceLogRepository;
import com.todayjikgwan.domain.game.*;
import com.todayjikgwan.domain.team.Stadium;
import com.todayjikgwan.domain.team.StadiumRepository;
import com.todayjikgwan.domain.team.Team;
import com.todayjikgwan.domain.team.TeamRepository;
import com.todayjikgwan.domain.user.User;
import com.todayjikgwan.domain.user.UserRepository;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 운영자 경기 관리 (REQ-F-601 ~ 604, REQ-F-607) */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminGameService {

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("M월 d일");

    private final GameRepository gameRepository;
    private final GameRevisionRepository revisionRepository;
    private final GameResultReportRepository reportRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final StadiumRepository stadiumRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final StatService statService;
    private final NotificationService notificationService;
    private final TodayJikgwanProperties properties;

    /** REQ-F-601 */
    @Transactional
    public Long register(int seasonYear, OffsetDateTime startAt,
                         Long stadiumId, Long homeTeamId, Long awayTeamId) {
        if (Objects.equals(homeTeamId, awayTeamId)) {
            throw new ApiException(ErrorCode.INVALID_GAME_TEAMS);
        }
        if (gameRepository.existsByGameDateAndStadiumIdAndHomeTeamIdAndAwayTeamId(
                startAt.toLocalDate(), stadiumId, homeTeamId, awayTeamId)) {
            throw new ApiException(ErrorCode.DUPLICATE_GAME);
        }
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        Team home = teamRepository.findById(homeTeamId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        Team away = teamRepository.findById(awayTeamId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        return gameRepository.save(Game.schedule(seasonYear, startAt, stadium, home, away)).getId();
    }

    /**
     * REQ-F-607 운영자 검토 대상.
     * 이미 시작했는데 결과가 확정되지 않은 경기를, 스코어별 제보 집계와 함께 보여준다.
     * 제보가 엇갈려 자동 확정 기준에 못 미친 경기를 사람이 판단해 확정하기 위한 화면이다.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> unconfirmed() {
        int threshold = properties.gameReport().confirmThreshold();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Game game : gameRepository.findUnconfirmedBefore(OffsetDateTime.now())) {
            List<Map<String, Object>> reports = reportRepository.tally(game.getId()).stream()
                    .map(t -> Map.<String, Object>of(
                            "homeScore", t.getHomeScore(),
                            "awayScore", t.getAwayScore(),
                            "count", t.getCnt()))
                    .toList();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("game", summarize(game));
            row.put("reports", reports);
            row.put("threshold", threshold);
            row.put("attendeeCount", attendanceLogRepository.findUserIdsByGame(game.getId()).size());
            result.add(row);
        }
        return result;
    }

    /**
     * REQ-F-602, REQ-F-603, REQ-F-309, REQ-F-604.
     *
     * <p>변경 전 값을 먼저 떠 두고 정정한 뒤 이력을 남긴다. 그리고 이 경기를 기록한
     * 모든 사용자의 전적을 다시 계산하고 변경 사실을 알린다. 정정은 남의 전적을 바꾸는
     * 일이라 조용히 끝내지 않는다.
     */
    @Transactional
    public Map<String, Object> revise(Long gameId, Long adminId,
                                      GameStatus status, Integer homeScore, Integer awayScore,
                                      String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ApiException(ErrorCode.REVISION_REASON_REQUIRED);
        }
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (status == GameStatus.FINISHED && (homeScore == null || awayScore == null)) {
            throw new ApiException(ErrorCode.REVISION_SCORE_REQUIRED);
        }
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));

        GameStatus beforeStatus = game.getStatus();
        Integer beforeHome = game.getHomeScore();
        Integer beforeAway = game.getAwayScore();

        game.revise(status, homeScore, awayScore);
        GameRevision revision = revisionRepository.save(
                GameRevision.of(game, beforeStatus, beforeHome, beforeAway, reason.trim(), admin));

        List<Long> affected = attendanceLogRepository.findUserIdsByGame(gameId);
        String label = game.getGameDate().format(LABEL) + " "
                + game.getAwayTeam().getShortName() + " vs " + game.getHomeTeam().getShortName();
        for (Long userId : affected) {
            statService.recalculate(userId);
            notificationService.notifyGameRevised(userId, label);
        }
        log.info("경기 {} 정정: {} {}:{} → {} {}:{} ({}명 재계산)", gameId,
                beforeStatus, beforeHome, beforeAway, status, homeScore, awayScore, affected.size());

        return Map.of("revisionId", revision.getId(), "recalculatedUsers", affected.size());
    }

    /** REQ-F-603 이력은 추가 전용이라 조회만 제공한다. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> revisions(Long gameId) {
        return revisionRepository.findByGame(gameId).stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("beforeStatus", r.getBeforeStatus());
                    m.put("beforeScore", score(r.getBeforeHomeScore(), r.getBeforeAwayScore()));
                    m.put("afterStatus", r.getAfterStatus());
                    m.put("afterScore", score(r.getAfterHomeScore(), r.getAfterAwayScore()));
                    m.put("reason", r.getReason());
                    m.put("revisedBy", r.getRevisedBy().getNickname());
                    m.put("revisedAt", r.getRevisedAt());
                    return m;
                })
                .toList();
    }

    private static String score(Integer home, Integer away) {
        return home == null || away == null ? null : home + ":" + away;
    }

    private Map<String, Object> summarize(Game game) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", game.getId());
        m.put("gameDate", game.getGameDate());
        m.put("startAt", game.getStartAt());
        m.put("stadium", game.getStadium().getName());
        m.put("homeTeam", game.getHomeTeam().getShortName());
        m.put("awayTeam", game.getAwayTeam().getShortName());
        m.put("homeScore", game.getHomeScore());
        m.put("awayScore", game.getAwayScore());
        m.put("status", game.getStatus());
        m.put("resultConfirmed", game.isResultConfirmed());
        return m;
    }
}
