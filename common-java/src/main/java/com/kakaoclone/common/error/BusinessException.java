package com.kakaoclone.common.error;

/**
 * 비즈니스 규칙 위반. {@link ErrorCode} 를 그대로 클라이언트에 전달합니다.
 *
 * <p>스택트레이스를 채우지 않습니다 — 예상된 흐름이라 비용만 들고 쓸모가 없습니다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.message());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message, null, false, false);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
