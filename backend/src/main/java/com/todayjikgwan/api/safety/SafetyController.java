package com.todayjikgwan.api.safety;

import com.todayjikgwan.domain.safety.ReportTarget;
import com.todayjikgwan.security.CurrentUser;
import com.todayjikgwan.service.SafetyService;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REQ-F-508, REQ-F-509 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SafetyController {

    private final SafetyService safetyService;

    @PostMapping("/reports")
    public ResponseEntity<Map<String, Long>> report(@RequestBody ReportRequest request) {
        Long id = safetyService.report(CurrentUser.id(), request.targetType(),
                request.targetId(), request.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("reportId", id));
    }

    @GetMapping("/users/me/blocks")
    public List<Map<String, Object>> blocks() {
        return safetyService.blocks(CurrentUser.id());
    }

    @PostMapping("/users/me/blocks")
    public ResponseEntity<Void> block(@RequestBody BlockRequest request) {
        safetyService.block(CurrentUser.id(), request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/users/me/blocks/{userId}")
    public ResponseEntity<Void> unblock(@PathVariable Long userId) {
        safetyService.unblock(CurrentUser.id(), userId);
        return ResponseEntity.noContent().build();
    }

    public record ReportRequest(@NotNull ReportTarget targetType, @NotNull Long targetId,
                                @NotNull String reason) { }

    public record BlockRequest(@NotNull Long userId) { }
}
