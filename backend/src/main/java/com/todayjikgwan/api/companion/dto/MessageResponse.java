package com.todayjikgwan.api.companion.dto;

import com.todayjikgwan.domain.companion.CompanionMessage;
import java.time.OffsetDateTime;

public record MessageResponse(Long id, String nickname, String content,
                              OffsetDateTime createdAt, boolean isMine) {

    public static MessageResponse of(CompanionMessage m, Long viewerId) {
        return new MessageResponse(m.getId(), m.getUser().getNickname(), m.getContent(),
                m.getCreatedAt(), m.getUser().getId().equals(viewerId));
    }
}
