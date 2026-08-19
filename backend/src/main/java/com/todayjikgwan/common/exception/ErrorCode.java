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
    OAUTH_PROVIDER_DISABLED(HttpStatus.BAD_REQUEST, "지원하지 않는 로그인 방식입니다."),
    OAUTH_FAILED(HttpStatus.UNAUTHORIZED, "소셜 로그인에 실패했습니다. 다시 시도해 주세요."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

    // 직관 기록
    DUPLICATE_ATTENDANCE_LOG(HttpStatus.CONFLICT, "이미 이 경기 기록이 있어요."),
    GAME_NOT_STARTED(HttpStatus.BAD_REQUEST, "아직 시작하지 않은 경기예요."),
    ZONE_NOT_IN_STADIUM(HttpStatus.BAD_REQUEST, "해당 구장의 좌석 구역이 아니에요."),
    CHEER_TEAM_NOT_IN_GAME(HttpStatus.BAD_REQUEST, "이 경기에 출전하지 않은 팀이에요."),
    SCORE_REPORT_REQUIRED(HttpStatus.BAD_REQUEST, "아직 결과가 확정되지 않은 경기예요. 스코어를 입력해 주세요."),

    // 직관 메이트
    COMPANION_POST_FULL(HttpStatus.CONFLICT, "방금 정원이 찼어요."),
    TIME_CONFLICT(HttpStatus.CONFLICT, "같은 시간에 확정된 약속이 있어요."),
    ALREADY_APPLIED(HttpStatus.CONFLICT, "이미 참여 중인 모집이에요."),
    SELF_APPLY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "본인이 등록한 모집에는 참여할 수 없어요."),
    POST_NOT_OPEN(HttpStatus.BAD_REQUEST, "모집이 마감되었어요."),
    GAME_ALREADY_STARTED(HttpStatus.BAD_REQUEST, "이미 시작한 경기예요."),
    CANCEL_DEADLINE_PASSED(HttpStatus.BAD_REQUEST, "경기 임박 시에는 취소할 수 없어요."),
    NOT_APPLIED(HttpStatus.BAD_REQUEST, "참여 중인 모집이 아니에요."),

    // 소통 · 안전
    NOT_CONFIRMED_MEMBER(HttpStatus.FORBIDDEN, "확정된 메이트만 대화할 수 있어요."),
    CHAT_READ_ONLY(HttpStatus.BAD_REQUEST, "종료된 대화방이에요."),
    ALREADY_REPORTED(HttpStatus.CONFLICT, "이미 신고한 대상이에요."),
    CANNOT_BLOCK_SELF(HttpStatus.BAD_REQUEST, "자기 자신은 차단할 수 없어요."),

    // 사진
    PHOTO_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "사진은 최대 10장까지 올릴 수 있어요."),

    // 공통
    INVALID_GAME_TEAMS(HttpStatus.BAD_REQUEST, "홈팀과 원정팀이 같을 수 없습니다."),
    DUPLICATE_GAME(HttpStatus.CONFLICT, "같은 날 같은 구장에 이미 등록된 경기입니다."),
    REVISION_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "정정 사유를 입력해 주세요."),
    REVISION_SCORE_REQUIRED(HttpStatus.BAD_REQUEST, "종료 처리하려면 양 팀 점수가 필요합니다."),
    DUPLICATE_ZONE(HttpStatus.CONFLICT, "같은 이름의 구역이 이미 있습니다."),
    ZONE_IN_USE(HttpStatus.CONFLICT, "관람 기록이 있는 구역은 지울 수 없어요. 비활성으로 돌려 주세요."),
    ZONE_INACTIVE(HttpStatus.BAD_REQUEST, "지금은 선택할 수 없는 구역이에요."),
    ZONE_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "구역 이름을 입력해 주세요."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다.");

    private final HttpStatus status;
    private final String message;
}
