package com.todayjikgwan.api.user;

import com.todayjikgwan.security.CurrentUser;
import com.todayjikgwan.service.UserService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
}
