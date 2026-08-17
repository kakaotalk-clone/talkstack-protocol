package com.kakaoclone.common.redis;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Lua 스크립트 원문을 클래스패스에서 읽어 제공합니다.
 *
 * <p>Spring 의 {@code RedisScript} 로 감싸는 일은 각 서비스가 합니다.
 * 이 모듈은 WebFlux(gateway) 와 MVC(chat) 가 함께 쓰므로 Spring 에 의존하지 않습니다.
 */
public final class LuaScripts {

    /** 방 단위 seq 발번. Redis 유실 시 DB 값에서 복구 */
    public static final String SEQ_ALLOCATE = load("/redis/seq_allocate.lua");

    /** 읽음 위치 갱신. 오래된 이벤트는 무시 */
    public static final String MARK_READ = load("/redis/mark_read.lua");

    private LuaScripts() {
    }

    private static String load(String path) {
        try (InputStream in = LuaScripts.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Lua 스크립트를 찾을 수 없습니다: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Lua 스크립트 로드 실패: " + path, e);
        }
    }
}
