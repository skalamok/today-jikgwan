package com.todayjikgwan.api.stat;

import com.todayjikgwan.api.stat.dto.StatItemResponse;
import com.todayjikgwan.api.stat.dto.StatSummaryResponse;
import com.todayjikgwan.domain.stat.StatDimension;
import com.todayjikgwan.security.CurrentUser;
import com.todayjikgwan.service.StatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @GetMapping("/me/summary")
    public StatSummaryResponse summary(@RequestParam(required = false) Integer season) {
        return statService.summary(CurrentUser.id(), season);
    }

    @GetMapping("/me")
    public List<StatItemResponse> byDimension(@RequestParam StatDimension dimension) {
        return statService.byDimension(CurrentUser.id(), dimension);
    }
}
