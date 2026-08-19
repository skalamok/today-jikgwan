package com.todayjikgwan.api.attendance;

import com.todayjikgwan.api.attendance.dto.AttendanceLogRequest;
import com.todayjikgwan.api.attendance.dto.AttendanceLogDetail;
import com.todayjikgwan.api.attendance.dto.AttendanceLogResponse;
import com.todayjikgwan.api.attendance.dto.PhotoUploadResponse;
import com.todayjikgwan.service.AttendancePhotoService;
import com.todayjikgwan.security.CurrentUser;
import com.todayjikgwan.service.AttendanceLogService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attendance-logs")
@RequiredArgsConstructor
public class AttendanceLogController {

    private final AttendanceLogService attendanceLogService;
    private final AttendancePhotoService attendancePhotoService;

    @PostMapping
    public ResponseEntity<AttendanceLogResponse> create(@Valid @RequestBody AttendanceLogRequest request) {
        AttendanceLogResponse response = attendanceLogService.create(CurrentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<AttendanceLogResponse> myLogs() {
        return attendanceLogService.myLogs(CurrentUser.id());
    }

    @GetMapping("/{logId}")
    public AttendanceLogDetail detail(@PathVariable Long logId) {
        return attendanceLogService.detail(CurrentUser.id(), logId);
    }

    /** REQ-F-212 기록 수정. 수정된 기록 전체를 돌려준다. */
    @PatchMapping("/{logId}")
    public AttendanceLogDetail update(@PathVariable Long logId,
                                      @Valid @RequestBody AttendanceLogRequest request) {
        return attendanceLogService.update(CurrentUser.id(), logId, request);
    }

    @DeleteMapping("/{logId}")
    public ResponseEntity<Void> delete(@PathVariable Long logId) {
        attendanceLogService.delete(CurrentUser.id(), logId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{logId}/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long logId, @PathVariable Long photoId) {
        attendanceLogService.deletePhoto(CurrentUser.id(), logId, photoId);
        return ResponseEntity.noContent().build();
    }

    /**
     * REQ-F-203 사진 업로드.
     * 저장 시 위치 메타데이터를 제거하고(REQ-NF-007) 썸네일을 생성한다(REQ-NF-002).
     */
    @PostMapping(value = "/{logId}/photos", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoUploadResponse> uploadPhotos(
            @PathVariable Long logId,
            @RequestPart("files") List<org.springframework.web.multipart.MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendancePhotoService.upload(CurrentUser.id(), logId, files));
    }
}
