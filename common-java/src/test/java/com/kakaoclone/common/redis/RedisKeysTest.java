package com.kakaoclone.common.redis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 키 포맷이 바뀌면 이미 Redis 에 쌓인 데이터를 못 찾습니다.
 * 세 서비스가 공유하는 계약이므로 문자열을 그대로 고정합니다.
 */
class RedisKeysTest {

    @Test
    void 키_포맷은_계약이므로_고정된다() {
        assertThat(RedisKeys.roomSeq(42)).isEqualTo("room:42:seq");
        assertThat(RedisKeys.roomRead(42)).isEqualTo("room:42:read");
        assertThat(RedisKeys.roomMembers(42)).isEqualTo("room:42:members");
        assertThat(RedisKeys.userConns(7)).isEqualTo("user:7:conns");
        assertThat(RedisKeys.userRooms(7)).isEqualTo("user:7:rooms");
        // 세션(기기)별로 나눠 저장합니다 — 유저당 하나면 다른 기기에서 로그인할 때
        // 앞 기기가 덮어써지고, 재사용 감지가 양쪽을 다 끊습니다.
        assertThat(RedisKeys.refreshToken(7, "sess-A")).isEqualTo("user:7:refresh:sess-A");
        assertThat(RedisKeys.userSessions(7)).isEqualTo("user:7:sessions");
        assertThat(RedisKeys.gatewayChannel("gw-1")).isEqualTo("gw.gw-1");
    }

    @Test
    void Lua_스크립트가_클래스패스에서_로드된다() {
        assertThat(LuaScripts.SEQ_ALLOCATE).contains("INCR");
        assertThat(LuaScripts.MARK_READ).contains("ZSCORE").contains("ZADD");
    }
}
