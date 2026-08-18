package com.todayjikgwan.api.companion.dto;

import com.todayjikgwan.domain.companion.CompanionComment;
import java.time.OffsetDateTime;

public record CommentResponse(Long id, String nickname, String content,
                              OffsetDateTime createdAt, boolean isMine) {

    public static CommentResponse of(CompanionComment c, Long viewerId) {
        return new CommentResponse(c.getId(), c.getUser().getNickname(), c.getContent(),
                c.getCreatedAt(), c.getUser().getId().equals(viewerId));
    }
}
