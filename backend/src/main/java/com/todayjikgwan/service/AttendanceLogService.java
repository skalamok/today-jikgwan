package com.todayjikgwan.service;

import com.todayjikgwan.api.attendance.dto.AttendanceLogRequest;
import com.todayjikgwan.api.attendance.dto.AttendanceLogDetail;
import com.todayjikgwan.api.attendance.dto.AttendanceLogResponse;
import com.todayjikgwan.domain.attendance.AttendancePhoto;
import com.todayjikgwan.domain.attendance.AttendancePhotoRepository;
import com.todayjikgwan.domain.weather.GameWeatherRepository;
import com.todayjikgwan.common.exception.ApiException;
import com.todayjikgwan.common.exception.ErrorCode;
import com.todayjikgwan.config.TodayJikgwanProperties;
import com.todayjikgwan.domain.attendance.AttendanceLog;
import com.todayjikgwan.domain.attendance.AttendanceLogRepository;
import com.todayjikgwan.domain.attendance.Visibility;
import com.todayjikgwan.domain.game.*;
import com.todayjikgwan.domain.stat.ZoneStat;
import com.todayjikgwan.domain.stat.ZoneStatRepository;
import com.todayjikgwan.domain.team.StadiumZone;
import com.todayjikgwan.domain.team.StadiumZoneRepository;
import com.todayjikgwan.domain.team.Team;
import com.todayjikgwan.domain.team.TeamRepository;
import com.todayjikgwan.domain.user.User;
import com.todayjikgwan.domain.user.UserRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceLogService {

    private final AttendanceLogRepository attendanceLogRepository;
    private final GameRepository gameRepository;
    private final GameResultReportRepository reportRepository;
    private final StadiumZoneRepository zoneRepository;
    private final ZoneStatRepository zoneStatRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final AttendancePhotoRepository photoRepository;
    private final GameWeatherRepository weatherRepository;
    private final BadgeService badgeService;
    private final StatService statService;
    private final TodayJikgwanProperties properties;

    /** REQ-F-201 ~ 211 */
    @Transactional
    public AttendanceLogResponse create(Long userId, AttendanceLogRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        Game game = gameRepository.findById(request.gameId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        // REQ-F-201 동일 경기 1건 제한
        attendanceLogRepository.findByUserIdAndGameId(userId, game.getId()).ifPresent(existing -> {
            throw new ApiException(ErrorCode.DUPLICATE_ATTENDANCE_LOG,
                    Map.of("existingLogId", existing.getId()));
        });

        // 아직 시작하지 않은 경기는 관람할 수 없다
        if (!game.isStarted()) {
            throw new ApiException(ErrorCode.GAME_NOT_STARTED);
        }

        StadiumZone zone = zoneRepository.findById(request.stadiumZoneId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!zone.getStadium().getId().equals(game.getStadium().getId())) {
            throw new ApiException(ErrorCode.ZONE_NOT_IN_STADIUM);
        }

        Team cheerTeam = null;
        if (request.cheerTeamId() != null) {
            if (!game.hasTeam(request.cheerTeamId())) {
                throw new ApiException(ErrorCode.CHEER_TEAM_NOT_IN_GAME);
            }
            cheerTeam = teamRepository.getReferenceById(request.cheerTeamId());
        }

        // REQ-F-606 결과 미확정 경기는 스코어 제보를 함께 받는다
        if (!game.isResultConfirmed()) {
            if (request.reportedHomeScore() == null || request.reportedAwayScore() == null) {
                throw new ApiException(ErrorCode.SCORE_REPORT_REQUIRED);
            }
            submitReport(game, user, request.reportedHomeScore(), request.reportedAwayScore());
        }

        AttendanceLog saved = attendanceLogRepository.save(AttendanceLog.builder()
                .user(user)
                .game(game)
                .cheerTeam(cheerTeam)
                .stadiumZone(zone)
                .zoneRating(request.zoneRating())
                .memo(request.memo())
                .gameRating(request.gameRating())
                .ticketCost(request.ticketCost())
                .foodCost(request.foodCost())
                .transportCost(request.transportCost())
                .visibility(request.visibility() == null
                        ? Visibility.PRIVATE : Visibility.valueOf(request.visibility()))
                .build());

        // REQ-F-216 예보는 사후 조회가 불가능하므로 기록 시점의 날씨를 복사해 보관한다
        weatherRepository.findById(game.getId()).ifPresent(w ->
                saved.applyWeather(w.getSkyCode(), w.getTemperature()));

        // REQ-F-110 구역별 만족도 집계에 즉시 반영
        ZoneStat zoneStat = zoneStatRepository.findById(zone.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        zoneStat.addRating(request.zoneRating());

        // REQ-F-309 전적 재집계
        statService.recalculate(userId);

        // REQ-F-703 마일스톤 배지 판정
        badgeService.evaluate(userId);

        return AttendanceLogResponse.from(saved);
    }

    /**
     * REQ-F-607 제보를 저장하고, 동일 스코어가 기준 수 이상 일치하면 결과를 확정한다.
     * 관람 기록 작성자만 제보하므로 일반 사용자 제보보다 신뢰도가 높다.
     */
    private void submitReport(Game game, User user, int home, int away) {
        if (reportRepository.existsByGameIdAndUserId(game.getId(), user.getId())) {
            return;
        }
        reportRepository.save(new GameResultReport(game, user, home, away));
        reportRepository.flush();

        List<GameResultReportRepository.ReportTally> tally = reportRepository.tally(game.getId());
        if (tally.isEmpty()) {
            return;
        }
        var top = tally.get(0);
        int confirmThreshold = properties.gameReport().confirmThreshold();
        if (top.getCnt() >= confirmThreshold) {
            game.confirmResult(top.getHomeScore(), top.getAwayScore(), GameSource.USER_REPORT);
            log.info("경기 {} 결과 확정: {}:{} (일치 제보 {}건)",
                    game.getId(), top.getHomeScore(), top.getAwayScore(), top.getCnt());
        }
    }

    /** REQ-F-214. 비공개 기록은 작성자만 조회할 수 있다 (REQ-N-008) */
    @Transactional(readOnly = true)
    public AttendanceLogDetail detail(Long userId, Long logId) {
        AttendanceLog log = attendanceLogRepository.findById(logId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        boolean mine = log.getUser().getId().equals(userId);
        if (!mine && log.getVisibility() != Visibility.PUBLIC) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return AttendanceLogDetail.from(log,
                photoRepository.findByAttendanceLogIdOrderBySortOrder(logId));
    }

    /** REQ-F-212. 삭제 후 전적을 재집계한다 */
    @Transactional
    public void delete(Long userId, Long logId) {
        AttendanceLog log = attendanceLogRepository.findById(logId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!log.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        // 구역 만족도 집계에서도 빼준다
        zoneStatRepository.findById(log.getStadiumZone().getId())
                .ifPresent(z -> z.removeRating(log.getZoneRating()));
        log.softDelete();
        statService.recalculate(userId);
    }

    /** REQ-F-215 사진 개별 삭제 */
    @Transactional
    public void deletePhoto(Long userId, Long logId, Long photoId) {
        AttendanceLog log = attendanceLogRepository.findById(logId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!log.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        photoRepository.findById(photoId)
                .filter(p -> p.getAttendanceLog().getId().equals(logId))
                .ifPresent(photoRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<AttendanceLogResponse> myLogs(Long userId) {
        return attendanceLogRepository.findMine(userId).stream()
                .map(AttendanceLogResponse::from)
                .toList();
    }
}
