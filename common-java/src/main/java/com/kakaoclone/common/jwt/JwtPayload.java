package com.kakaoclone.common.jwt;

/**
 * 액세스 토큰에 담기는 최소 정보.
 *
 * <p>Gateway 는 핸드셰이크에서 이 값만 뽑아 커넥션에 붙이고, 그 뒤로는
 * DB 를 보지 않습니다. 닉네임 같은 표시용 정보는 넣지 않습니다 — 바뀌면
 * 토큰이 만료될 때까지 낡은 값이 돌아다니기 때문입니다.
 */
public record JwtPayload(long userId, String loginId, String sessionId) {

    /**
     * 액세스 토큰용 — 세션 id 가 필요 없습니다.
     *
     * <p>액세스 토큰은 30분이면 만료되므로 개별 무효화가 의미가 없습니다.
     * 세션 id 는 <b>리프레시 토큰에만</b> 실립니다.
     */
    public JwtPayload(long userId, String loginId) {
        this(userId, loginId, null);
    }

    public JwtPayload withSession(String sessionId) {
        return new JwtPayload(userId, loginId, sessionId);
    }
}
