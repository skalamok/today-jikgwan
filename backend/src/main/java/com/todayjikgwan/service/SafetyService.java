package com.todayjikgwan.service;

import com.todayjikgwan.common.exception.ApiException;
import com.todayjikgwan.common.exception.ErrorCode;
import com.todayjikgwan.domain.safety.*;
import com.todayjikgwan.domain.user.User;
import com.todayjikgwan.domain.user.UserRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 신고 · 차단 (REQ-F-508, REQ-F-509) */
@Service
@RequiredArgsConstructor
public class SafetyService {

    private final ReportRepository reportRepository;
    private final UserBlockRepository blockRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long report(Long userId, ReportTarget type, Long targetId, String reason) {
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(userId, type, targetId)) {
            throw new ApiException(ErrorCode.ALREADY_REPORTED);
        }
        User reporter = userRepository.getReferenceById(userId);
        return reportRepository.save(new Report(reporter, type, targetId, reason)).getId();
    }

    @Transactional
    public void block(Long userId, Long blockedId) {
        if (userId.equals(blockedId)) {
            throw new ApiException(ErrorCode.CANNOT_BLOCK_SELF);
        }
        if (blockRepository.existsByUserIdAndBlockedId(userId, blockedId)) {
            return;
        }
        userRepository.findById(blockedId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        blockRepository.save(new UserBlock(userId, blockedId));
    }

    @Transactional
    public void unblock(Long userId, Long blockedId) {
        blockRepository.deleteByUserIdAndBlockedId(userId, blockedId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> blocks(Long userId) {
        return blockRepository.findByUserId(userId).stream()
                .map(b -> Map.<String, Object>of(
                        "userId", b.getBlockedId(),
                        "nickname", userRepository.findById(b.getBlockedId())
                                .map(User::getNickname).orElse("(탈퇴한 사용자)"),
                        "blockedAt", b.getCreatedAt().toString()))
                .toList();
    }
}
