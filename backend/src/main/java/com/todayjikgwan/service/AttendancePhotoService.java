package com.todayjikgwan.service;

import com.todayjikgwan.api.attendance.dto.PhotoUploadResponse;
import com.todayjikgwan.common.exception.ApiException;
import com.todayjikgwan.common.exception.ErrorCode;
import com.todayjikgwan.config.TodayJikgwanProperties;
import com.todayjikgwan.domain.attendance.AttendanceLog;
import com.todayjikgwan.domain.attendance.AttendanceLogRepository;
import com.todayjikgwan.domain.attendance.AttendancePhoto;
import com.todayjikgwan.domain.attendance.AttendancePhotoRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendancePhotoService {

    private final AttendanceLogRepository attendanceLogRepository;
    private final AttendancePhotoRepository photoRepository;
    private final ImageStorageService imageStorageService;
    private final TodayJikgwanProperties properties;

    @Transactional
    public PhotoUploadResponse upload(Long userId, Long logId, List<MultipartFile> files) {
        AttendanceLog log = attendanceLogRepository.findById(logId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        // REQ-NF-008 본인 기록만 수정할 수 있다
        if (!log.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        int max = properties.storage().maxPhotosPerLog();
        long already = photoRepository.countByAttendanceLogId(logId);
        if (already + files.size() > max) {
            throw new ApiException(ErrorCode.PHOTO_LIMIT_EXCEEDED,
                    java.util.Map.of("current", already, "max", max));
        }

        List<PhotoUploadResponse.Uploaded> uploaded = new ArrayList<>();
        List<PhotoUploadResponse.Failed> failed = new ArrayList<>();
        int order = (int) already;

        for (MultipartFile file : files) {
            String name = file.getOriginalFilename();
            try {
                if (file.isEmpty()) {
                    failed.add(new PhotoUploadResponse.Failed(name, "EMPTY_FILE"));
                    continue;
                }
                if (file.getSize() > properties.storage().maxFileSizeBytes()) {
                    failed.add(new PhotoUploadResponse.Failed(name, "FILE_TOO_LARGE"));
                    continue;
                }
                if (!imageStorageService.isAllowed(file.getContentType())) {
                    failed.add(new PhotoUploadResponse.Failed(name, "UNSUPPORTED_TYPE"));
                    continue;
                }
                var stored = imageStorageService.store(file.getBytes(), file.getContentType());
                AttendancePhoto photo = photoRepository.save(new AttendancePhoto(
                        log, stored.originalUrl(), stored.thumbnailUrl(),
                        stored.takenAt(), stored.size(), stored.mimeType(), order++));
                uploaded.add(new PhotoUploadResponse.Uploaded(
                        photo.getId(), photo.getOriginalUrl(), photo.getThumbnailUrl(), photo.getTakenAt()));
            } catch (Exception e) {
                AttendancePhotoService.log.warn("사진 업로드 실패 {}: {}", name, e.getMessage());
                failed.add(new PhotoUploadResponse.Failed(name, "STORAGE_ERROR"));
            }
        }
        return new PhotoUploadResponse(uploaded, failed);
    }
}
