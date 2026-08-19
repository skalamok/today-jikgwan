package com.todayjikgwan.service;

import com.todayjikgwan.api.companion.dto.ApplyResponse;
import com.todayjikgwan.common.exception.ApiException;
import com.todayjikgwan.common.exception.ErrorCode;
import com.todayjikgwan.domain.companion.*;
import com.todayjikgwan.domain.user.User;
import com.todayjikgwan.domain.user.UserRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메이트 확정 1회 시도.
 *
 * <p>재시도할 때마다 새 트랜잭션에서 실행되어야 하므로 {@link CompanionService} 와
 * 별도 빈으로 분리했다. 같은 빈 안에서 자기 메서드를 호출하면 프록시를 거치지 않아
 * REQUIRES_NEW 가 적용되지 않는다.
 */
@Component
@RequiredArgsConstructor
public class CompanionApplyExecutor {

    /** REQ-F-505 같은 시간대 판정 폭 */
    private static final int CONFLICT_WINDOW_HOURS = 4;

    private final CompanionPostRepository postRepository;
    private final CompanionApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApplyResponse applyOnce(Long postId, Long userId) {
        CompanionPost post = postRepository.findDetail(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        if (post.getAuthor().getId().equals(userId)) {
            throw new ApiException(ErrorCode.SELF_APPLY_NOT_ALLOWED);
        }
        if (post.getGame().isStarted()) {
            throw new ApiException(ErrorCode.GAME_ALREADY_STARTED);
        }
        if (!post.isOpen()) {
            throw new ApiException(ErrorCode.COMPANION_POST_FULL);
        }
        applicationRepository.findByCompanionPostIdAndUserId(postId, userId).ifPresent(a -> {
            if (a.getStatus() == ApplicationStatus.CONFIRMED) {
                throw new ApiException(ErrorCode.ALREADY_APPLIED);
            }
        });

        // REQ-F-505 동일 시간대 중복 확정 차단
        OffsetDateTime start = post.getGame().getStartAt();
        long conflicts = applicationRepository.countTimeConflicts(
                userId, post.getGame().getId(),
                start.minusHours(CONFLICT_WINDOW_HOURS), start.plusHours(CONFLICT_WINDOW_HOURS));
        if (conflicts > 0) {
            throw new ApiException(ErrorCode.TIME_CONFLICT);
        }

        User user = userRepository.getReferenceById(userId);
        int seq = post.confirmOne();                 // 정원 검사 + 카운트 증가
        applicationRepository.save(new CompanionApplication(post, user, seq));
        postRepository.saveAndFlush(post);           // 여기서 @Version 충돌이 감지된다

        notificationService.notifyCompanionConfirmed(userId,
                "%s vs %s".formatted(post.getGame().getHomeTeam().getShortName(),
                                     post.getGame().getAwayTeam().getShortName()), postId);

        return new ApplyResponse(seq, post.getConfirmedCount(), post.getCapacity());
    }
}
