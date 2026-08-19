package com.todayjikgwan.domain.attendance;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** REQ-F-209 함께 간 사람 */
public interface AttendanceCompanionRepository extends JpaRepository<AttendanceCompanion, Long> {

    List<AttendanceCompanion> findByAttendanceLogId(Long attendanceLogId);

    void deleteByAttendanceLogId(Long attendanceLogId);
}
