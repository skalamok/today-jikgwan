package com.todayjikgwan.service;

import com.todayjikgwan.api.stadium.dto.*;
import com.todayjikgwan.common.exception.ApiException;
import com.todayjikgwan.common.exception.ErrorCode;
import com.todayjikgwan.config.TodayJikgwanProperties;
import com.todayjikgwan.domain.attendance.AttendanceLogRepository;
import com.todayjikgwan.domain.stat.StatDimension;
import com.todayjikgwan.domain.stat.UserStat;
import com.todayjikgwan.domain.stat.UserStatRepository;
import com.todayjikgwan.domain.stat.ZoneStat;
import com.todayjikgwan.domain.stat.ZoneStatRepository;
import com.todayjikgwan.domain.team.*;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** REQ-F-108 ~ 111 */
@Service
@RequiredArgsConstructor
public class StadiumService {

    private final StadiumRepository stadiumRepository;
    private final StadiumZoneRepository zoneRepository;
    private final ZoneStatRepository zoneStatRepository;
    private final TeamRepository teamRepository;
    private final UserStatRepository userStatRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final TodayJikgwanProperties properties;

    @Transactional(readOnly = true)
    public List<StadiumResponse> list() {
        return stadiumRepository.findAll().stream()
                .map(s -> new StadiumResponse(s.getId(), s.getName(), s.getNameEn(), s.getCapacity(),
                        String.join(" · ", homeTeamNames(s.getId()))))
                .toList();
    }

    @Transactional(readOnly = true)
    public StadiumDetailResponse detail(Long stadiumId, Long userId) {
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        int threshold = properties.stat().smallSampleThreshold();
        List<ZoneStatResponse> zones = zoneRepository.findByStadiumIdAndActiveTrueOrderBySortOrder(stadiumId).stream()
                .map(z -> {
                    ZoneStat st = zoneStatRepository.findById(z.getId()).orElse(null);
                    int count = st == null ? 0 : st.getRatingCount();
                    boolean small = count < threshold;
                    Double avg = (st == null || st.getAvgRating() == null || small)
                            ? null : st.getAvgRating().doubleValue();
                    return new ZoneStatResponse(z.getId(), z.getName(), avg, count, small);
                })
                // 표본이 많은 구역을 위로
                .sorted(Comparator.comparingInt(ZoneStatResponse::ratingCount).reversed())
                .toList();

        StadiumDetailResponse.MyRecord myRecord = null;
        if (userId != null) {
            myRecord = userStatRepository
                    .findByUserIdAndDimensionAndDimensionKeyAndSeasonYear(
                            userId, StatDimension.STADIUM, String.valueOf(stadiumId), UserStat.SEASON_ALL)
                    .map(s -> new StadiumDetailResponse.MyRecord(
                            s.getGames(), s.getWins(), s.getDraws(), s.getLosses()))
                    .orElse(null);
        }

        return new StadiumDetailResponse(stadium.getId(), stadium.getName(), stadium.getNameEn(),
                stadium.getCapacity(), homeTeamNames(stadiumId), zones, myRecord);
    }

    /** REQ-F-111. 공개 기록만, 작성자 미노출. */
    @Transactional(readOnly = true)
    public List<ZoneReviewResponse> zoneReviews(Long zoneId, int size) {
        return attendanceLogRepository
                .findPublicReviewsByZone(zoneId, PageRequest.of(0, size)).stream()
                .map(l -> new ZoneReviewResponse(
                        l.getMemo(), l.getZoneRating(), l.getGame().getGameDate()))
                .toList();
    }

    private List<String> homeTeamNames(Long stadiumId) {
        return teamRepository.findByHomeStadiumId(stadiumId).stream()
                .map(Team::getName)
                .toList();
    }
}
