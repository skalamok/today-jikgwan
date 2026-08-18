package com.todayjikgwan.domain.safety;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTarget type, Long targetId);
    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);
    long countByTargetTypeAndTargetId(ReportTarget type, Long targetId);
}
