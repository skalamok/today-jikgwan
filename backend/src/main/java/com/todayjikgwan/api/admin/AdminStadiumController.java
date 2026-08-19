package com.todayjikgwan.api.admin;

import com.todayjikgwan.service.AdminStadiumService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REQ-F-605. 경로 전체가 ROLE_ADMIN 이다 (SecurityConfig, REQ-NF-009) */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminStadiumController {

    private final AdminStadiumService adminStadiumService;

    @GetMapping("/stadiums/{stadiumId}/zones")
    public List<Map<String, Object>> zones(@PathVariable Long stadiumId) {
        return adminStadiumService.zones(stadiumId);
    }

    @PostMapping("/stadiums/{stadiumId}/zones")
    public ResponseEntity<Map<String, Long>> addZone(@PathVariable Long stadiumId,
                                                     @RequestBody ZoneRequest request) {
        Long id = adminStadiumService.addZone(stadiumId, request.name(), request.sortOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("zoneId", id));
    }

    @PatchMapping("/zones/{zoneId}")
    public void updateZone(@PathVariable Long zoneId, @RequestBody ZoneRequest request) {
        adminStadiumService.updateZone(zoneId, request.name(), request.sortOrder(), request.active());
    }

    @DeleteMapping("/zones/{zoneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteZone(@PathVariable Long zoneId) {
        adminStadiumService.deleteZone(zoneId);
    }

    @PatchMapping("/stadiums/{stadiumId}")
    public void updateStadium(@PathVariable Long stadiumId, @RequestBody StadiumRequest request) {
        adminStadiumService.updateStadium(stadiumId, request.address(), request.capacity(),
                request.gridNx(), request.gridNy());
    }

    public record ZoneRequest(String name, Integer sortOrder, Boolean active) { }

    public record StadiumRequest(String address, Integer capacity, Integer gridNx, Integer gridNy) { }
}
