package com.todayjikgwan.service;

import com.todayjikgwan.api.companion.dto.ChatResponse;
import com.todayjikgwan.api.companion.dto.CommentResponse;
import com.todayjikgwan.api.companion.dto.MessageResponse;
import com.todayjikgwan.common.exception.ApiException;
import com.todayjikgwan.common.exception.ErrorCode;
import com.todayjikgwan.domain.companion.*;
import com.todayjikgwan.domain.user.User;
import com.todayjikgwan.domain.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 동행 소통 (REQ-F-510 ~ 513).
 *
 * <p>확정 전 문의는 공개 댓글로, 확정 후 약속 조율은 비공개 대화방으로 나눈다.
 * 하나로 합치면 확정자끼리의 약속이 아무나 볼 수 있게 되고,
 * 대화방만 두면 지원 전 질문을 할 수 없다.
 */
@Service
@RequiredArgsConstructor
public class CompanionChatService {

    /** REQ-F-513 경기 종료 후 이 기간이 지나면 읽기 전용으로 전환한다 */
    private static final int READ_ONLY_AFTER_DAYS = 7;

    private final CompanionPostRepository postRepository;
    private final CompanionApplicationRepository applicationRepository;
    private final CompanionCommentRepository commentRepository;
    private final CompanionMessageRepository messageRepository;
    private final CompanionMessageReadRepository readRepository;
    private final UserRepository userRepository;

    // ---------- 공개 댓글 (REQ-F-510) ----------

    @Transactional(readOnly = true)
    public List<CommentResponse> comments(Long postId, Long viewerId) {
        return commentRepository.findByPost(postId).stream()
                .map(c -> CommentResponse.of(c, viewerId)).toList();
    }

    @Transactional
    public CommentResponse addComment(Long postId, Long userId, String content) {
        CompanionPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        User user = userRepository.getReferenceById(userId);
        CompanionComment saved = commentRepository.save(new CompanionComment(post, user, content));
        return CommentResponse.of(saved, userId);
    }

    // ---------- 확정자 대화방 (REQ-F-511 ~ 513) ----------

    @Transactional(readOnly = true)
    public ChatResponse messages(Long postId, Long userId, OffsetDateTime after) {
        CompanionPost post = requireMember(postId, userId);
        List<CompanionMessage> raw = (after == null)
                ? messageRepository.findAllByPost(postId)
                : messageRepository.findByPostAfter(postId, after);
        List<MessageResponse> messages = raw.stream()
                .map(m -> MessageResponse.of(m, userId)).toList();

        OffsetDateTime lastRead = readRepository.findByCompanionPostIdAndUserId(postId, userId)
                .map(CompanionMessageRead::getLastReadAt)
                .orElse(post.getCreatedAt());
        long unread = messageRepository.countUnread(postId, userId, lastRead);

        return new ChatResponse(messages, isReadOnly(post), unread);
    }

    @Transactional
    public MessageResponse send(Long postId, Long userId, String content) {
        CompanionPost post = requireMember(postId, userId);
        if (isReadOnly(post)) {
            throw new ApiException(ErrorCode.CHAT_READ_ONLY);
        }
        User user = userRepository.getReferenceById(userId);
        CompanionMessage saved = messageRepository.save(new CompanionMessage(post, user, content));
        markRead(postId, userId);
        return MessageResponse.of(saved, userId);
    }

    @Transactional
    public void markRead(Long postId, Long userId) {
        readRepository.findByCompanionPostIdAndUserId(postId, userId)
                .ifPresentOrElse(CompanionMessageRead::touch,
                        () -> readRepository.save(new CompanionMessageRead(postId, userId)));
    }

    /** 작성자이거나 확정된 지원자만 대화에 접근할 수 있다 */
    private CompanionPost requireMember(Long postId, Long userId) {
        CompanionPost post = postRepository.findDetail(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (post.getAuthor().getId().equals(userId)) {
            return post;
        }
        boolean confirmed = applicationRepository.findByCompanionPostIdAndUserId(postId, userId)
                .filter(a -> a.getStatus() == ApplicationStatus.CONFIRMED)
                .isPresent();
        if (!confirmed) {
            throw new ApiException(ErrorCode.NOT_CONFIRMED_MEMBER);
        }
        return post;
    }

    /** REQ-F-513 */
    private boolean isReadOnly(CompanionPost post) {
        return post.getGame().getStartAt().plusDays(READ_ONLY_AFTER_DAYS)
                .isBefore(OffsetDateTime.now());
    }
}
