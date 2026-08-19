package com.todayjikgwan.service;

import com.todayjikgwan.api.attendance.dto.AttendanceLogRequest;
import com.todayjikgwan.api.attendance.dto.AttendanceLogDetail;
import com.todayjikgwan.api.attendance.dto.AttendanceLogResponse;
import com.todayjikgwan.domain.attendance.AttendanceCompanion;
import com.todayjikgwan.domain.attendance.AttendanceCompanionRepository;
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
    private final StadiumZoneRepository zoneRepository;
    private final ZoneStatRepository zoneStatRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final AttendancePhotoRepository photoRepository;
    private final AttendanceCompanionRepository companionRepository;
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
        // 비활성 구역은 목록에서 빠지지만 id 를 직접 보내는 경로가 남아 있다 (REQ-F-605)
        if (!zone.isActive()) {
            throw new ApiException(ErrorCode.ZONE_INACTIVE);
        }

        Team cheerTeam = null;
        if (request.cheerTeamId() != null) {
            if (!game.hasTeam(request.cheerTeamId())) {
                throw new ApiException(ErrorCode.CHEER_TEAM_NOT_IN_GAME);
            }
            cheerTeam = teamRepository.getReferenceById(request.cheerTeamId());
        }

        // REQ-NF-015. 경기 결과는 운영자만 등록·정정한다. 기록 작성이 결과에 영향을 주지 않는다.
        // 결과가 아직 없는 경기도 기록은 남길 수 있으며, 전적 집계에서만 제외된다 (REQ-F-606).

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

        replaceCompanions(saved, request.companions());   // REQ-F-209

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


    /** REQ-F-214. 비공개 기록은 작성자만 조회할 수 있다 (REQ-NF-008) */
    @Transactional(readOnly = true)
    public AttendanceLogDetail detail(Long userId, Long logId) {
        AttendanceLog log = attendanceLogRepository.findById(logId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        boolean mine = log.getUser().getId().equals(userId);
        if (!mine && log.getVisibility() != Visibility.PUBLIC) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return AttendanceLogDetail.from(log,
                photoRepository.findByAttendanceLogIdOrderBySortOrder(logId),
                companionRepository.findByAttendanceLogId(logId).stream()
                        .map(AttendanceCompanion::displayName).toList());
    }

    /**
     * REQ-F-209 함께 간 사람을 다시 쓴다.
     *
     * 한 명씩 고쳐 넣는 대신 통째로 지우고 다시 넣는다. 목록이 짧고 순서에 뜻이 없어
     * 어느 줄이 바뀌었는지 따지는 것보다 이 편이 단순하다.
     */
    private void replaceCompanions(AttendanceLog log, List<AttendanceLogRequest.Companion> input) {
        companionRepository.deleteByAttendanceLogId(log.getId());
        if (input == null || input.isEmpty()) {
            return;
        }
        for (AttendanceLogRequest.Companion c : input) {
            User member = c.userId() == null ? null
                    : userRepository.findById(c.userId())
                            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
            String name = c.name() == null ? null : c.name().trim();
            // 회원도 이름도 없으면 남길 것이 없다
            if (member == null && (name == null || name.isEmpty())) {
                continue;
            }
            companionRepository.save(new AttendanceCompanion(log, member, name));
        }
    }

    /**
     * REQ-F-212 기록 수정.
     *
     * 작성과 같은 검증을 다시 거친다. 수정으로 다른 값이 되면 파생 데이터가 함께 움직여야 한다.
     * 구역이 바뀌면 이전 구역의 만족도 집계에서 빼고 새 구역에 더하고, 응원팀이나 경기가
     * 바뀌면 승패 판정이 달라지므로 전적을 다시 계산한다.
     */
    @Transactional
    public AttendanceLogDetail update(Long userId, Long logId, AttendanceLogRequest request) {
        AttendanceLog log = attendanceLogRepository.findById(logId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!log.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        if (log.getDeletedAt() != null) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        Game game = gameRepository.findById(request.gameId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        // 경기를 옮길 때만 중복을 다시 본다. 자기 자신은 걸리면 안 된다 (REQ-F-201)
        if (!game.getId().equals(log.getGame().getId())) {
            attendanceLogRepository.findByUserIdAndGameId(userId, game.getId())
                    .filter(other -> !other.getId().equals(logId))
                    .ifPresent(other -> {
                        throw new ApiException(ErrorCode.DUPLICATE_ATTENDANCE_LOG,
                                Map.of("existingLogId", other.getId()));
                    });
        }
        if (!game.isStarted()) {
            throw new ApiException(ErrorCode.GAME_NOT_STARTED);
        }

        StadiumZone zone = zoneRepository.findById(request.stadiumZoneId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!zone.getStadium().getId().equals(game.getStadium().getId())) {
            throw new ApiException(ErrorCode.ZONE_NOT_IN_STADIUM);
        }
        // 이미 고른 구역이 나중에 비활성이 된 경우까지 막으면 수정을 못 한다.
        // 구역을 바꾸는 경우에만 활성 여부를 본다 (REQ-F-605)
        boolean zoneChanged = !zone.getId().equals(log.getStadiumZone().getId());
        if (zoneChanged && !zone.isActive()) {
            throw new ApiException(ErrorCode.ZONE_INACTIVE);
        }

        Team cheerTeam = null;
        if (request.cheerTeamId() != null) {
            if (!game.hasTeam(request.cheerTeamId())) {
                throw new ApiException(ErrorCode.CHEER_TEAM_NOT_IN_GAME);
            }
            cheerTeam = teamRepository.getReferenceById(request.cheerTeamId());
        }

        // REQ-F-110 구역 만족도 집계 이동. 같은 구역이라도 점수가 바뀌면 다시 반영한다
        Short beforeRating = log.getZoneRating();
        Long beforeZoneId = log.getStadiumZone().getId();
        if (zoneChanged || !beforeRating.equals(request.zoneRating())) {
            zoneStatRepository.findById(beforeZoneId)
                    .ifPresent(z -> z.removeRating(beforeRating));
            ZoneStat after = zoneStatRepository.findById(zone.getId())
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
            after.addRating(request.zoneRating());
        }

        log.update(game, cheerTeam, zone, request.zoneRating(), request.memo(),
                request.gameRating(), request.ticketCost(), request.foodCost(),
                request.transportCost(),
                request.visibility() == null ? null : Visibility.valueOf(request.visibility()));

        replaceCompanions(log, request.companions());   // REQ-F-209

        // REQ-F-216 경기를 옮기면 그 경기의 날씨로 다시 채운다
        weatherRepository.findById(game.getId())
                .ifPresent(w -> log.applyWeather(w.getSkyCode(), w.getTemperature()));

        // REQ-F-309 응원팀 · 경기 · 비용이 바뀌면 집계가 달라진다
        statService.recalculate(userId);
        badgeService.evaluate(userId);

        return detail(userId, logId);
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
