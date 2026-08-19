package com.todayjikgwan.service;

import com.todayjikgwan.common.exception.ApiException;
import com.todayjikgwan.common.exception.ErrorCode;
import com.todayjikgwan.domain.attendance.AttendanceLogRepository;
import com.todayjikgwan.domain.team.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영자 구장 · 좌석 구역 관리 (REQ-F-605).
 *
 * <p><b>구역은 지우지 않고 비활성화만 한다.</b> 구역은 관람 기록이 참조하고 만족도 집계의
 * 단위이기도 해서, 지우면 과거 기록의 좌석 정보와 그 구역에 쌓인 평가가 함께 사라진다.
 * 더 쓰지 않을 구역은 비활성으로 돌려 새 기록에서만 선택되지 않게 한다.
 */
@Service
@RequiredArgsConstructor
public class AdminStadiumService {

    private final StadiumRepository stadiumRepository;
    private final StadiumZoneRepository zoneRepository;
    private final AttendanceLogRepository attendanceLogRepository;

    /** 비활성 구역도 함께 준다. 운영자는 되살릴 수 있어야 한다. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> zones(Long stadiumId) {
        return zoneRepository.findByStadiumIdOrderBySortOrder(stadiumId).stream()
                .map(z -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", z.getId());
                    m.put("name", z.getName());
                    m.put("sortOrder", z.getSortOrder());
                    m.put("active", z.isActive());
                    m.put("logCount", attendanceLogRepository.countByStadiumZoneId(z.getId()));
                    return m;
                })
                .toList();
    }

    @Transactional
    public Long addZone(Long stadiumId, String name, Integer sortOrder) {
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        String trimmed = requireName(name);
        if (zoneRepository.existsByStadiumIdAndName(stadiumId, trimmed)) {
            throw new ApiException(ErrorCode.DUPLICATE_ZONE);
        }
        int order = sortOrder != null ? sortOrder
                : zoneRepository.findByStadiumIdOrderBySortOrder(stadiumId).size();
        return zoneRepository.save(StadiumZone.create(stadium, trimmed, order)).getId();
    }

    @Transactional
    public void updateZone(Long zoneId, String name, Integer sortOrder, Boolean active) {
        StadiumZone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        if (name != null && !name.isBlank() && !name.trim().equals(zone.getName())) {
            String trimmed = requireName(name);
            if (zoneRepository.existsByStadiumIdAndName(zone.getStadium().getId(), trimmed)) {
                throw new ApiException(ErrorCode.DUPLICATE_ZONE);
            }
            zone.rename(trimmed);
        }
        if (sortOrder != null) {
            zone.reorder(sortOrder);
        }
        if (active != null) {
            zone.setActive(active);
        }
    }

    /** 구장 자체는 기상청 격자와 관람 기록이 함께 걸려 있어 정보 수정만 제공한다. */
    @Transactional
    public void updateStadium(Long stadiumId, String address, Integer capacity,
                              Integer gridNx, Integer gridNy) {
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        stadium.updateInfo(address, capacity, gridNx, gridNy);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new ApiException(ErrorCode.ZONE_NAME_REQUIRED);
        }
        return name.trim();
    }
}
