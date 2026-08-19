package com.todayjikgwan.service;

import com.todayjikgwan.api.stat.dto.StatItemResponse;
import com.todayjikgwan.api.stat.dto.StatSummaryResponse;
import com.todayjikgwan.config.TodayJikgwanProperties;
import com.todayjikgwan.domain.attendance.AttendanceCompanion;
import com.todayjikgwan.domain.attendance.AttendanceCompanionRepository;
import com.todayjikgwan.domain.attendance.AttendanceLog;
import com.todayjikgwan.domain.attendance.AttendanceLogRepository;
import com.todayjikgwan.domain.game.GameResult;
import com.todayjikgwan.domain.stat.*;
import com.todayjikgwan.domain.team.StadiumRepository;
import com.todayjikgwan.domain.team.TeamRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전적 집계 (REQ-F-301 ~ 309).
 *
 * 재계산 방식은 <b>전체 재계산</b>을 채택했다. 개인 관람 기록은 시즌당 수십 건 수준이라
 * 증분 반영의 이점보다 정합성이 중요하고, 경기 결과 정정(REQ-F-602)처럼 과거 데이터가
 * 바뀌는 경우에도 같은 경로로 처리할 수 있기 때문이다.
 * 데이터가 커지면 증분 반영으로 전환한다.
 */
@Service
@RequiredArgsConstructor
public class StatService {

    private final AttendanceLogRepository attendanceLogRepository;
    private final UserStatRepository userStatRepository;
    private final UserStreakRepository userStreakRepository;
    private final StadiumRepository stadiumRepository;
    private final TeamRepository teamRepository;
    private final AttendanceCompanionRepository companionRepository;
    private final com.todayjikgwan.domain.user.UserRepository userRepository;
    private final TodayJikgwanProperties properties;

    /**
     * 차원별 집계(구장별·상대팀별·요일별)에만 적용하는 최소 표본.
     *
     * <p>통산 전적에는 적용하지 않는다. 통산은 "1경기 1승"처럼 모수가 함께 보여서
     * 승률만 따로 떼어 읽히지 않지만, 차원별은 여러 항목이 나란히 놓여 순위처럼 읽힌다.
     * 잠실 2경기 2승(1.000)과 고척 1경기 1패(0.000)를 나란히 보여주면 실제로는
     * 아무 의미 없는 서열이 생긴다.
     */
    private int threshold() {
        return properties.stat().smallSampleThreshold();
    }

    /** REQ-F-309 기록 변경 또는 경기 결과 정정 시 호출한다. */
    @Transactional
    public void recalculate(Long userId) {
        userStatRepository.deleteByUserId(userId);
        userStatRepository.flush();

        List<AttendanceLog> logs = attendanceLogRepository.findMine(userId);
        Map<String, UserStat> buffer = new HashMap<>();

        for (AttendanceLog log : logs) {
            GameResult result = log.result();
            int cost = log.totalCost();
            int season = log.getGame().getSeasonYear();
            Long opponentId = opponentOf(log);

            accumulate(buffer, userId, StatDimension.TOTAL, "ALL", UserStat.SEASON_ALL, result, cost);
            accumulate(buffer, userId, StatDimension.SEASON, String.valueOf(season), season, result, cost);
            accumulate(buffer, userId, StatDimension.STADIUM,
                    String.valueOf(log.getGame().getStadium().getId()), UserStat.SEASON_ALL, result, cost);
            if (opponentId != null) {
                accumulate(buffer, userId, StatDimension.OPPONENT,
                        String.valueOf(opponentId), UserStat.SEASON_ALL, result, cost);
            }
            accumulate(buffer, userId, StatDimension.DAY_OF_WEEK,
                    log.getGame().getGameDate().getDayOfWeek().name(), UserStat.SEASON_ALL, result, cost);

            // REQ-F-307 함께 간 사람별. 한 기록에 여러 명이면 각자에게 같은 경기가 한 번씩 들어간다.
            // 비용은 첫 사람에게만 더한다. 사람 수만큼 곱해지면 총액이 실제보다 커진다
            boolean firstCompanion = true;
            for (AttendanceCompanion c : companionRepository.findByAttendanceLogId(log.getId())) {
                String key = c.getCompanionUser() != null
                        ? "u:" + c.getCompanionUser().getId()
                        : "n:" + c.getCompanionName();
                accumulate(buffer, userId, StatDimension.COMPANION, key, UserStat.SEASON_ALL,
                        result, firstCompanion ? cost : 0);
                firstCompanion = false;
            }

            // REQ-F-310 날씨별. 기록 시점에 예보를 받아 둔 경기에만 값이 있다
            if (log.getWeatherSky() != null && !log.getWeatherSky().isBlank()) {
                accumulate(buffer, userId, StatDimension.WEATHER, log.getWeatherSky(),
                        UserStat.SEASON_ALL, result, cost);
            }
        }
        userStatRepository.saveAll(buffer.values());
        recalculateStreak(userId, logs);
    }

    private Long opponentOf(AttendanceLog log) {
        Long cheer = log.cheerTeamId();
        if (cheer == null) {
            return null;                       // 중립 관람은 상대팀 개념이 없다
        }
        var game = log.getGame();
        return game.getHomeTeam().getId().equals(cheer)
                ? game.getAwayTeam().getId()
                : game.getHomeTeam().getId();
    }

    private void accumulate(Map<String, UserStat> buffer, Long userId, StatDimension dimension,
                            String key, int season, GameResult result, int cost) {
        String bufferKey = dimension + "|" + key + "|" + season;
        buffer.computeIfAbsent(bufferKey, k -> new UserStat(userId, dimension, key, season))
              .apply(result, cost);
    }

    /** REQ-F-306. 무승부는 연승을 끊지 않는다. */
    private void recalculateStreak(Long userId, List<AttendanceLog> logs) {
        List<AttendanceLog> ordered = new ArrayList<>(logs);
        ordered.sort(Comparator.comparing(l -> l.getGame().getGameDate()));

        int current = 0;
        int longest = 0;
        LocalDate last = null;
        for (AttendanceLog log : ordered) {
            GameResult r = log.result();
            if (r == GameResult.NEUTRAL) {
                continue;
            }
            switch (r) {
                case WIN -> current = current >= 0 ? current + 1 : 1;
                case LOSE -> current = current <= 0 ? current - 1 : -1;
                case DRAW -> { }                        // 유지
                default -> { }
            }
            longest = Math.max(longest, current);
            last = log.getGame().getGameDate();
        }
        UserStreak streak = userStreakRepository.findById(userId).orElseGet(() -> new UserStreak(userId));
        streak.update(current, longest, last);
        userStreakRepository.save(streak);
    }

    @Transactional(readOnly = true)
    public StatSummaryResponse summary(Long userId, Integer season) {
        int seasonKey = season == null ? UserStat.SEASON_ALL : season;
        StatDimension dimension = season == null ? StatDimension.TOTAL : StatDimension.SEASON;
        String key = season == null ? "ALL" : String.valueOf(season);

        UserStat stat = userStatRepository
                .findByUserIdAndDimensionAndDimensionKeyAndSeasonYear(userId, dimension, key, seasonKey)
                .orElse(new UserStat(userId, dimension, key, seasonKey));

        int neutral = (int) attendanceLogRepository.findMine(userId).stream()
                .filter(l -> l.result() == GameResult.NEUTRAL)
                .count();

        UserStreak streak = userStreakRepository.findById(userId).orElseGet(() -> new UserStreak(userId));
        // 통산 전적은 1경기부터 승률을 낸다. 첫 기록에서 바로 성취가 보이는 편이
        // 다음 기록을 남길 이유가 된다 (REQ-F-705 와 같은 맥락).
        boolean hasGames = stat.getGames() > 0;

        return new StatSummaryResponse(
                season, stat.getGames(), stat.getWins(), stat.getDraws(), stat.getLosses(),
                hasGames ? winRate(stat) : null, !hasGames, neutral,
                streak.getCurrentStreak(), streak.getLongestWin(), stat.getTotalCost());
    }

    @Transactional(readOnly = true)
    public List<StatItemResponse> byDimension(Long userId, StatDimension dimension) {
        return userStatRepository
                .findByUserIdAndDimensionAndSeasonYear(userId, dimension, UserStat.SEASON_ALL)
                .stream()
                .sorted(Comparator.comparingInt(UserStat::getGames).reversed())
                .map(s -> {
                    boolean small = s.getGames() < threshold();
                    return new StatItemResponse(s.getDimensionKey(), label(dimension, s.getDimensionKey()),
                            s.getGames(), s.getWins(), s.getDraws(), s.getLosses(),
                            small ? null : winRate(s), small);
                })
                .toList();
    }

    /** 차원 키(ID·요일)를 화면에 노출할 이름으로 변환한다. */
    private String label(StatDimension dimension, String key) {
        return switch (dimension) {
            case STADIUM -> stadiumRepository.findById(Long.valueOf(key))
                    .map(st -> st.getName()).orElse(key);
            case OPPONENT -> teamRepository.findById(Long.valueOf(key))
                    .map(t -> t.getName()).orElse(key);
            case DAY_OF_WEEK -> DayOfWeek.valueOf(key)
                    .getDisplayName(TextStyle.SHORT, Locale.KOREAN);
            case SEASON -> key + " 시즌";
            // 회원은 지금 닉네임을 따라가고, 비회원은 적어 둔 이름을 그대로 쓴다
            case COMPANION -> key.startsWith("u:")
                    ? userRepository.findById(Long.valueOf(key.substring(2)))
                            .map(u -> u.getNickname()).orElse("(탈퇴한 사용자)")
                    : key.substring(2);
            default -> key;
        };
    }

    /** 무승부를 제외한 승률. 야구 관례에 따라 승 / (승 + 패). */
    private Double winRate(UserStat s) {
        int decided = s.getWins() + s.getLosses();
        if (decided == 0) {
            return null;
        }
        return Math.round((double) s.getWins() / decided * 1000) / 1000.0;
    }
}
