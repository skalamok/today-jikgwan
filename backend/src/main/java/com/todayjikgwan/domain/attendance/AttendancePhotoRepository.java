package com.todayjikgwan.domain.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendancePhotoRepository extends JpaRepository<AttendancePhoto, Long> {
    long countByAttendanceLogId(Long attendanceLogId);
}
