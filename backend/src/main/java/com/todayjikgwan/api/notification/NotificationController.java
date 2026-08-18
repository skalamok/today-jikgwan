package com.todayjikgwan.api.notification;

import com.todayjikgwan.security.CurrentUser;
import com.todayjikgwan.service.BadgeService;
import com.todayjikgwan.service.NotificationService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final BadgeService badgeService;

    /** REQ-F-706 */
    @GetMapping("/notifications")
    public Map<String, Object> list() {
        return notificationService.list(CurrentUser.id());
    }

    /** REQ-F-707 */
    @PutMapping("/notifications/read")
    public ResponseEntity<Void> readAll() {
        notificationService.readAll(CurrentUser.id());
        return ResponseEntity.noContent().build();
    }

    /** REQ-F-703 */
    @GetMapping("/users/me/badges")
    public List<Map<String, Object>> badges() {
        return badgeService.myBadges(CurrentUser.id());
    }
}
