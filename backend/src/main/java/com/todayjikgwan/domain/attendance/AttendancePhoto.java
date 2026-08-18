package com.todayjikgwan.domain.attendance;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "attendance_photos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendancePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_log_id", nullable = false)
    private AttendanceLog attendanceLog;

    @Column(name = "original_url", nullable = false, length = 500)
    private String originalUrl;

    @Column(name = "thumbnail_url", nullable = false, length = 500)
    private String thumbnailUrl;

    /** REQ-F-204 경기 자동 매칭용. 위치 메타데이터는 저장 전에 제거한다 (REQ-N-007). */
    @Column(name = "taken_at")
    private OffsetDateTime takenAt;

    @Column(name = "file_size")
    private Integer fileSize;

    @Column(name = "mime_type", length = 50)
    private String mimeType;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public AttendancePhoto(AttendanceLog log, String originalUrl, String thumbnailUrl,
                           OffsetDateTime takenAt, Integer fileSize, String mimeType, int sortOrder) {
        this.attendanceLog = log;
        this.originalUrl = originalUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.takenAt = takenAt;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.sortOrder = sortOrder;
    }
}
