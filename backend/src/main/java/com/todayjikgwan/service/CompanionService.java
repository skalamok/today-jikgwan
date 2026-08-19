package com.todayjikgwan.service;

import com.todayjikgwan.api.companion.dto.ApplyResponse;
import com.todayjikgwan.api.companion.dto.CompanionPostCreateRequest;
import com.todayjikgwan.api.companion.dto.CompanionPostResponse;
import com.todayjikgwan.common.exception.ApiException;
import com.todayjikgwan.common.exception.ErrorCode;
import com.todayjikgwan.domain.companion.*;
import com.todayjikgwan.domain.game.Game;
import com.todayjikgwan.domain.game.GameRepository;
import com.todayjikgwan.domain.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanionService {

    /** 낙관적 락 충돌 시 재시도 횟수 */
    private static final int MAX_RETRY = 5;

    /** REQ-F-506 경기 시작 24시간 이내에는 취소할 수 없다 */
    private static final int CANCEL_DEADLINE_HOURS = 24;

    private final CompanionPostRepository postRepository;
    private final CompanionApplicationRepository applicationRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final CompanionApplyExecutor applyExecutor;

    /** REQ-F-501 */
    @Transactional
    public Long createPost(Long userId, CompanionPostCreateRequest request) {
        Game game = gameRepository.findById(request.gameId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (game.isStarted()) {
            throw new ApiException(ErrorCode.GAME_ALREADY_STARTED);
        }
        CompanionPost post = new CompanionPost(
                game, userRepository.getReferenceById(userId), request.capacity(), request.intro());
        return postRepository.save(post).getId();
    }

    /**
     * REQ-F-504 선착순 확정.
     *
     * <p>동시 요청에서 낙관적 락 충돌이 나면 다시 읽어 정원을 확인하고 재시도한다.
     * 재시도 중 정원이 차면 그 시점에 409(COMPANION_POST_FULL)로 응답한다.
     * DB의 ck_post_capacity CHECK 제약이 최종 방어선이므로,
     * 애플리케이션 로직이 뚫려도 정원 초과 데이터는 저장되지 않는다.
     */
    public ApplyResponse apply(Long postId, Long userId) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                return applyExecutor.applyOnce(postId, userId);
            } catch (OptimisticLockingFailureException e) {
                log.debug("메이트 확정 경합 발생 postId={} userId={} attempt={}", postId, userId, attempt);
            } catch (DataIntegrityViolationException e) {
                // DB CHECK 제약에 걸린 경우 = 정원 초과
                throw new ApiException(ErrorCode.COMPANION_POST_FULL);
            }
        }
        throw new ApiException(ErrorCode.COMPANION_POST_FULL);
    }

    /** REQ-F-506 */
    @Transactional
    public void cancel(Long postId, Long userId) {
        CompanionApplication application = applicationRepository
                .findByCompanionPostIdAndUserId(postId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_APPLIED));
        if (application.getStatus() == ApplicationStatus.CANCELED) {
            throw new ApiException(ErrorCode.NOT_APPLIED);
        }
        CompanionPost post = postRepository.findDetail(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        if (post.getGame().getStartAt().minusHours(CANCEL_DEADLINE_HOURS)
                .isBefore(OffsetDateTime.now())) {
            throw new ApiException(ErrorCode.CANCEL_DEADLINE_PASSED);
        }
        application.cancel();
        post.releaseOne();
    }

    @Transactional(readOnly = true)
    public List<CompanionPostResponse> list(boolean includeClosed, Long userId) {
        return postRepository.findAllForList(includeClosed).stream()
                .map(p -> CompanionPostResponse.of(p, myStatus(p, userId), List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanionPostResponse detail(Long postId, Long userId) {
        CompanionPost post = postRepository.findDetail(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        List<CompanionPostResponse.Member> members = applicationRepository
                .findByCompanionPostIdAndStatusOrderBySeq(postId, ApplicationStatus.CONFIRMED)
                .stream()
                .map(a -> new CompanionPostResponse.Member(
                        a.getUser().getNickname(), a.getSeq(), false))
                .toList();

        List<CompanionPostResponse.Member> all = new java.util.ArrayList<>();
        all.add(new CompanionPostResponse.Member(post.getAuthor().getNickname(), 1, true));
        all.addAll(members);
        return CompanionPostResponse.of(post, myStatus(post, userId), all);
    }

    /** 화면의 버튼 상태를 결정하기 위한 값 (SCR-MATE-002 상태 정의) */
    private String myStatus(CompanionPost post, Long userId) {
        if (userId == null) {
            return "NONE";
        }
        if (post.getAuthor().getId().equals(userId)) {
            return "AUTHOR";
        }
        return applicationRepository.findByCompanionPostIdAndUserId(post.getId(), userId)
                .filter(a -> a.getStatus() == ApplicationStatus.CONFIRMED)
                .map(a -> "CONFIRMED")
                .orElse("NONE");
    }
}
