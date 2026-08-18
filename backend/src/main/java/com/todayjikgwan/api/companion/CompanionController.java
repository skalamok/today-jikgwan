package com.todayjikgwan.api.companion;

import com.todayjikgwan.api.companion.dto.ApplyResponse;
import com.todayjikgwan.api.companion.dto.CompanionPostCreateRequest;
import com.todayjikgwan.api.companion.dto.ChatResponse;
import com.todayjikgwan.api.companion.dto.CommentResponse;
import com.todayjikgwan.api.companion.dto.CompanionPostResponse;
import com.todayjikgwan.api.companion.dto.MessageResponse;
import com.todayjikgwan.service.CompanionChatService;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
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
    private final CompanionChatService chatService;

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

    // ---------- 공개 문의 댓글 (REQ-F-510) ----------

    @GetMapping("/{postId}/comments")
    public List<CommentResponse> comments(@PathVariable Long postId) {
        return chatService.comments(postId, CurrentUser.id());
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long postId,
                                                      @RequestBody ContentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.addComment(postId, CurrentUser.id(), request.content()));
    }

    // ---------- 확정자 대화방 (REQ-F-511 ~ 513) ----------

    /** after 를 넘기면 그 이후 메시지만 반환한다. 주기적 조회로 실시간을 대체할 수 있다 */
    @GetMapping("/{postId}/messages")
    public ChatResponse messages(
            @PathVariable Long postId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime after) {
        return chatService.messages(postId, CurrentUser.id(), after);
    }

    @PostMapping("/{postId}/messages")
    public ResponseEntity<MessageResponse> send(@PathVariable Long postId,
                                                @RequestBody ContentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.send(postId, CurrentUser.id(), request.content()));
    }

    @PutMapping("/{postId}/messages/read")
    public ResponseEntity<Void> markRead(@PathVariable Long postId) {
        chatService.markRead(postId, CurrentUser.id());
        return ResponseEntity.noContent().build();
    }

    public record ContentRequest(String content) { }

    @DeleteMapping("/{postId}/applications")
    public ResponseEntity<Void> cancel(@PathVariable Long postId) {
        companionService.cancel(postId, CurrentUser.id());
        return ResponseEntity.noContent().build();
    }
}
