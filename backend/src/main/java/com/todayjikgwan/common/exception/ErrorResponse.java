package com.todayjikgwan.common.exception;

public record ErrorResponse(String code, String message, Object data) {

    public static ErrorResponse of(ErrorCode errorCode, Object data) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), data);
    }
}
