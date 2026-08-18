package com.todayjikgwan.api.stadium;

import com.todayjikgwan.api.stadium.dto.*;
import com.todayjikgwan.service.StadiumService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stadiums")
@RequiredArgsConstructor
public class StadiumController {

    private final StadiumService stadiumService;

    @GetMapping
    public List<StadiumResponse> list() {
        return stadiumService.list();
    }

    @GetMapping("/{stadiumId}")
    public StadiumDetailResponse detail(@PathVariable Long stadiumId) {
        return stadiumService.detail(stadiumId, optionalUserId());
    }

    @GetMapping("/{stadiumId}/zones/{zoneId}/reviews")
    public List<ZoneReviewResponse> reviews(@PathVariable Long stadiumId,
                                            @PathVariable Long zoneId,
                                            @RequestParam(defaultValue = "10") int size) {
        return stadiumService.zoneReviews(zoneId, size);
    }

    /** 비로그인도 열람 가능한 화면이므로 인증 정보가 없으면 null 을 반환한다. */
    private Long optionalUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getPrincipal() instanceof Long id) ? id : null;
    }
}
