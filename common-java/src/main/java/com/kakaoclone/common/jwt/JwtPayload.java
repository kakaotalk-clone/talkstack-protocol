package com.kakaoclone.common.jwt;

/**
 * 액세스 토큰에 담기는 최소 정보.
 *
 * <p>Gateway 는 핸드셰이크에서 이 값만 뽑아 커넥션에 붙이고, 그 뒤로는
 * DB 를 보지 않습니다. 닉네임 같은 표시용 정보는 넣지 않습니다 — 바뀌면
 * 토큰이 만료될 때까지 낡은 값이 돌아다니기 때문입니다.
 */
public record JwtPayload(long userId, String loginId) {
}
