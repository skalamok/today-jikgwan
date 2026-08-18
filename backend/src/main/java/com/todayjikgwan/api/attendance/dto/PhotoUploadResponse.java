package com.todayjikgwan.api.attendance.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * REQ-F-203. 일부 파일이 실패해도 나머지는 정상 처리하고 실패 목록을 함께 돌려준다.
 */
public record PhotoUploadResponse(List<Uploaded> uploaded, List<Failed> failed) {

    public record Uploaded(Long id, String originalUrl, String thumbnailUrl, OffsetDateTime takenAt) { }

    public record Failed(String fileName, String reason) { }
}
