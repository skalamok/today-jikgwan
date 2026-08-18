package com.todayjikgwan.api.stadium.dto;

import java.time.LocalDate;

/** REQ-F-111. 공개 설정된 기록의 메모만 노출하며 작성자는 표시하지 않는다. */
public record ZoneReviewResponse(String memo, short rating, LocalDate attendedAt) {
}
