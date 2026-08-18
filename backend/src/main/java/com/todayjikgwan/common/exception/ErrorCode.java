package com.todayjikgwan.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** API 명세(openapi.yaml)의 ErrorResponse.code 와 1:1로 대응한다. */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 인증
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

    // 직관 기록
    DUPLICATE_ATTENDANCE_LOG(HttpStatus.CONFLICT, "이미 이 경기 기록이 있어요."),
    GAME_NOT_STARTED(HttpStatus.BAD_REQUEST, "아직 시작하지 않은 경기예요."),
    ZONE_NOT_IN_STADIUM(HttpStatus.BAD_REQUEST, "해당 구장의 좌석 구역이 아니에요."),
    CHEER_TEAM_NOT_IN_GAME(HttpStatus.BAD_REQUEST, "이 경기에 출전하지 않은 팀이에요."),
    SCORE_REPORT_REQUIRED(HttpStatus.BAD_REQUEST, "아직 결과가 확정되지 않은 경기예요. 스코어를 입력해 주세요."),

    // 동행
    COMPANION_POST_FULL(HttpStatus.CONFLICT, "방금 정원이 찼어요."),
    TIME_CONFLICT(HttpStatus.CONFLICT, "같은 시간에 확정된 동행이 있어요."),
    ALREADY_APPLIED(HttpStatus.CONFLICT, "이미 참여 중인 모집이에요."),
    SELF_APPLY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "본인이 등록한 모집에는 참여할 수 없어요."),
    POST_NOT_OPEN(HttpStatus.BAD_REQUEST, "모집이 마감되었어요."),
    GAME_ALREADY_STARTED(HttpStatus.BAD_REQUEST, "이미 시작한 경기예요."),
    CANCEL_DEADLINE_PASSED(HttpStatus.BAD_REQUEST, "경기 임박 시에는 취소할 수 없어요."),
    NOT_APPLIED(HttpStatus.BAD_REQUEST, "참여 중인 모집이 아니에요."),

    // 사진
    PHOTO_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "사진은 최대 10장까지 올릴 수 있어요."),

    // 공통
    NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다.");

    private final HttpStatus status;
    private final String message;
}
