package com.kakaoclone.common.error;

/**
 * 서비스 공통 에러 코드.
 *
 * <p>REST 응답 · WS ERROR 프레임 · gRPC 상태에 같은 이름이 실려 나갑니다.
 * 클라이언트는 HTTP 상태가 아니라 이 코드로 분기합니다.
 */
public enum ErrorCode {

    // 400
    INVALID_REQUEST(400, "요청이 올바르지 않습니다"),
    INVALID_FRAME(400, "알 수 없는 프레임입니다"),
    UNSUPPORTED_PROTOCOL_VERSION(400, "지원하지 않는 프로토콜 버전입니다"),

    // 401 / 403
    UNAUTHORIZED(401, "인증이 필요합니다"),
    TOKEN_EXPIRED(401, "토큰이 만료되었습니다"),
    INVALID_TOKEN(401, "유효하지 않은 토큰입니다"),
    NOT_ROOM_MEMBER(403, "채팅방 참여자가 아닙니다"),

    // 404
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다"),
    ROOM_NOT_FOUND(404, "채팅방을 찾을 수 없습니다"),

    // 409
    DUPLICATE_LOGIN_ID(409, "이미 사용 중인 아이디입니다"),
    ALREADY_FRIEND(409, "이미 친구입니다"),

    // 429
    RATE_LIMITED(429, "요청이 너무 많습니다"),

    // 500
    INTERNAL_ERROR(500, "서버 오류가 발생했습니다"),
    UPSTREAM_UNAVAILABLE(503, "일시적으로 서비스를 사용할 수 없습니다");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int status() {
        return status;
    }

    public String message() {
        return message;
    }
}
