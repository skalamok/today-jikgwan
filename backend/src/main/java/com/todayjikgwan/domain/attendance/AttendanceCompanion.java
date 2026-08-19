package com.todayjikgwan.domain.attendance;

import com.todayjikgwan.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * REQ-F-209 함께 간 사람.
 *
 * <p>서비스 회원이면 사용자로, 아니면 이름만 남긴다. 같이 간 사람이 모두 이 서비스를
 * 쓰지는 않으므로 회원만 받으면 기록이 반쪽이 된다.
 */
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

    public AttendanceCompanion(AttendanceLog log, User companionUser, String companionName) {
        this.attendanceLog = log;
        this.companionUser = companionUser;
        this.companionName = companionName;
    }

    /** 화면에 보일 이름. 회원이면 지금 닉네임을 따라간다 */
    public String displayName() {
        return companionUser != null ? companionUser.getNickname() : companionName;
    }
}
