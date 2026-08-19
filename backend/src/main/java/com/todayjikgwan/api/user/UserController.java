package com.todayjikgwan.api.user;

import com.todayjikgwan.security.CurrentUser;
import com.todayjikgwan.service.UserService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REQ-F-005 응원팀 설정 / REQ-F-006 프로필 */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Map<String, Object> me() {
        return userService.me(CurrentUser.id());
    }

    @PatchMapping
    public Map<String, Object> update(@RequestBody UpdateRequest request) {
        return userService.update(CurrentUser.id(), request.nickname(), request.favoriteTeamId());
    }

    public record UpdateRequest(String nickname, Long favoriteTeamId) { }

    /** REQ-F-007 탈퇴. 기록은 익명으로 남는다 */
    @DeleteMapping
    public ResponseEntity<Void> withdraw() {
        userService.withdraw(CurrentUser.id());
        return ResponseEntity.noContent().build();
    }
}
