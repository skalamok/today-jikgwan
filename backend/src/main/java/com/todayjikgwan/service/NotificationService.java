package com.todayjikgwan.service;

import com.todayjikgwan.domain.notification.Notification;
import com.todayjikgwan.domain.notification.NotificationRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** REQ-F-604, REQ-F-706, REQ-F-707 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    @Transactional
    public void notifyBadge(Long userId, String badgeName) {
        repository.save(new Notification(userId, "BADGE_ACHIEVED",
                "새 배지를 받았어요", badgeName, "/my"));
    }

    @Transactional
    public void notifyCompanionConfirmed(Long userId, String gameLabel, Long postId) {
        repository.save(new Notification(userId, "COMPANION_CONFIRMED",
                "메이트가 확정됐어요", gameLabel, "/companions/" + postId));
    }

    /** REQ-F-604 경기 결과 정정으로 전적이 바뀐 사용자에게 안내 */
    @Transactional
    public void notifyGameRevised(Long userId, String gameLabel) {
        repository.save(new Notification(userId, "GAME_REVISED",
                "경기 결과가 정정됐어요", gameLabel + " 결과 변경으로 전적이 갱신됐습니다", "/stats"));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(Long userId) {
        List<Map<String, Object>> items = repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> Map.<String, Object>of(
                        "id", n.getId(), "type", n.getType(), "title", n.getTitle(),
                        "body", n.getBody() == null ? "" : n.getBody(),
                        "linkUrl", n.getLinkUrl() == null ? "" : n.getLinkUrl(),
                        "read", n.getReadAt() != null,
                        "createdAt", n.getCreatedAt().toString()))
                .toList();
        return Map.of("items", items, "unreadCount", repository.countByUserIdAndReadAtIsNull(userId));
    }

    @Transactional
    public void readAll(Long userId) {
        repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(n -> n.getReadAt() == null)
                .forEach(Notification::read);
    }
}
