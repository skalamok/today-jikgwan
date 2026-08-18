package com.todayjikgwan.api.companion;

import com.todayjikgwan.api.companion.dto.ApplyResponse;
import com.todayjikgwan.api.companion.dto.CompanionPostCreateRequest;
import com.todayjikgwan.api.companion.dto.CompanionPostResponse;
import com.todayjikgwan.security.CurrentUser;
import com.todayjikgwan.service.CompanionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/companion-posts")
@RequiredArgsConstructor
public class CompanionController {

    private final CompanionService companionService;

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(@Valid @RequestBody CompanionPostCreateRequest request) {
        Long id = companionService.createPost(CurrentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("postId", id));
    }

    @GetMapping
    public List<CompanionPostResponse> list(
            @RequestParam(defaultValue = "false") boolean includeClosed) {
        return companionService.list(includeClosed, CurrentUser.id());
    }

    @GetMapping("/{postId}")
    public CompanionPostResponse detail(@PathVariable Long postId) {
        return companionService.detail(postId, CurrentUser.id());
    }

    /** REQ-F-504 선착순 확정 */
    @PostMapping("/{postId}/applications")
    public ResponseEntity<ApplyResponse> apply(@PathVariable Long postId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companionService.apply(postId, CurrentUser.id()));
    }

    @DeleteMapping("/{postId}/applications")
    public ResponseEntity<Void> cancel(@PathVariable Long postId) {
        companionService.cancel(postId, CurrentUser.id());
        return ResponseEntity.noContent().build();
    }
}
