package com.todayjikgwan.service;

import com.todayjikgwan.domain.attendance.AttendanceLog;
import com.todayjikgwan.domain.attendance.AttendanceLogRepository;
import com.todayjikgwan.domain.badge.*;
import com.todayjikgwan.domain.game.GameResult;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 마일스톤 배지 판정 (REQ-F-703).
 *
 * <p>기록이 저장될 때마다 조건을 다시 평가한다. 이미 받은 배지는 건너뛴다.
 * <b>기록 1건만으로도 성취가 발생하도록</b> 첫 기록·첫 승리를 낮은 문턱으로 두었다.
 * 데이터가 쌓여야 가치가 생기는 서비스에서 라이트 사용자에게 줄 것이 없으면 안 되기 때문이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private static final int ALL_STADIUM_COUNT = 9;

    private final AttendanceLogRepository attendanceLogRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final NotificationService notificationService;

    @Transactional
    public List<String> evaluate(Long userId) {
        List<AttendanceLog> logs = attendanceLogRepository.findMine(userId);
        if (logs.isEmpty()) {
            return List.of();
        }
        Set<String> owned = userBadgeRepository.findCodes(userId);
        List<String> newly = new ArrayList<>();

        Set<Long> stadiums = new HashSet<>();
        int wins = 0, streak = 0, bestStreak = 0;
        boolean away = false;
        Map<Integer, Integer> perSeason = new HashMap<>();

        List<AttendanceLog> ordered = new ArrayList<>(logs);
        ordered.sort(Comparator.comparing(l -> l.getGame().getGameDate()));

        for (AttendanceLog l : ordered) {
            stadiums.add(l.getGame().getStadium().getId());
            perSeason.merge(l.getGame().getSeasonYear(), 1, Integer::sum);

            Long cheer = l.cheerTeamId();
            if (cheer != null) {
                // 응원팀의 홈구장이 아닌 곳에서 본 경기
                Long homeStadium = l.getGame().getHomeTeam().getId().equals(cheer)
                        ? l.getGame().getStadium().getId() : null;
                if (homeStadium == null) {
                    away = true;
                }
            }
            GameResult r = l.result();
            if (r == GameResult.WIN) {
                wins++; streak = Math.max(streak, 0) + 1; bestStreak = Math.max(bestStreak, streak);
            } else if (r == GameResult.LOSE) {
                streak = 0;
            }
        }

        grant(userId, owned, newly, "FIRST_LOG", true);
        grant(userId, owned, newly, "FIRST_WIN", wins >= 1);
        grant(userId, owned, newly, "FIRST_AWAY", away);
        grant(userId, owned, newly, "ALL_STADIUMS", stadiums.size() >= ALL_STADIUM_COUNT);
        grant(userId, owned, newly, "STREAK_3", bestStreak >= 3);
        grant(userId, owned, newly, "TEN_GAMES",
                perSeason.values().stream().anyMatch(c -> c >= 10));
        return newly;
    }

    private void grant(Long userId, Set<String> owned, List<String> newly, String code, boolean ok) {
        if (!ok || owned.contains(code)) {
            return;
        }
        badgeRepository.findByCode(code).ifPresent(b -> {
            userBadgeRepository.save(new UserBadge(userId, b));
            newly.add(code);
            notificationService.notifyBadge(userId, b.getName());
            log.info("배지 부여 userId={} code={}", userId, code);
        });
    }

    /**
     * 조회 시에도 조건을 다시 평가한다.
     * 이미 받은 배지는 건너뛰므로 멱등이며, 배지 로직이 추가되기 전에 쌓인 기록도 반영된다.
     */
    @Transactional
    public List<Map<String, Object>> myBadges(Long userId) {
        evaluate(userId);
        List<Badge> all = badgeRepository.findAll();
        Set<String> owned = userBadgeRepository.findCodes(userId);
        Map<String, java.time.OffsetDateTime> at = new HashMap<>();
        userBadgeRepository.findMine(userId)
                .forEach(ub -> at.put(ub.getBadge().getCode(), ub.getAchievedAt()));

        return all.stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", b.getCode());
            m.put("name", b.getName());
            m.put("description", b.getDescription());
            m.put("achieved", owned.contains(b.getCode()));
            m.put("achievedAt", at.containsKey(b.getCode()) ? at.get(b.getCode()).toString() : null);
            return m;
        }).toList();
    }
}
