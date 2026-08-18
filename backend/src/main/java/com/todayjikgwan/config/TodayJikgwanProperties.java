package com.todayjikgwan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "todayjikgwan")
public record TodayJikgwanProperties(Storage storage, Stat stat, GameReport gameReport) {

    /** 업로드 파일 저장 (REQ-F-203, REQ-N-002, REQ-N-007) */
    public record Storage(String baseDir, String publicBaseUrl,
                          int maxPhotosPerLog, long maxFileSizeBytes, int thumbnailMaxPx) { }

    /** REQ-F-305 소표본 표시 정책 */
    public record Stat(int smallSampleThreshold) { }

    /** REQ-F-607 제보 확정 기준 */
    public record GameReport(int confirmThreshold) { }
}
