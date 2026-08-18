package com.todayjikgwan.api.companion.dto;

import java.util.List;

/** REQ-F-511 ~ 513 */
public record ChatResponse(List<MessageResponse> messages, boolean readOnly, long unreadCount) {
}
