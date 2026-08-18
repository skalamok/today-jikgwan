package com.todayjikgwan.api.plan;

import com.todayjikgwan.api.plan.dto.PlanCreateRequest;
import com.todayjikgwan.api.plan.dto.PlanGenerateResponse;
import com.todayjikgwan.security.CurrentUser;
import com.todayjikgwan.service.ViewingPlanService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/viewing-plans")
@RequiredArgsConstructor
public class ViewingPlanController {

    private final ViewingPlanService viewingPlanService;

    /** REQ-F-401, REQ-F-402 */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody PlanCreateRequest request) {
        Long id = viewingPlanService.create(CurrentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("planId", id, "stadiumIds", request.stadiumIds() == null
                        ? List.of() : request.stadiumIds()));
    }

    /** REQ-F-403 후보 일정 자동 편성 */
    @PostMapping("/{planId}/generate")
    public PlanGenerateResponse generate(@PathVariable Long planId,
                                         @RequestBody(required = false) GenerateOptions options) {
        List<Long> stadiumIds = options == null ? null : options.stadiumIds();
        return viewingPlanService.generate(CurrentUser.id(), planId, stadiumIds);
    }

    public record GenerateOptions(List<Long> stadiumIds) { }
}
