package com.todayjikgwan.domain.attendance;

import com.todayjikgwan.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** REQ-F-209. 회원 동행자와 비회원(이름만) 동행자를 모두 지원한다. */
@Getter
@Entity
@Table(name = "attendance_companions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceCompanion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_log_id", nullable = false)
    private AttendanceLog attendanceLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "companion_user_id")
    private User companionUser;

    @Column(name = "companion_name", length = 30)
    private String companionName;
}
