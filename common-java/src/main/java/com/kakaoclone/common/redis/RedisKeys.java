package com.kakaoclone.common.redis;

/**
 * Redis 키 네이밍의 단일 원천.
 *
 * <p>gateway · chat-app · chat-consumer 세 서비스가 같은 Redis 를 봅니다.
 * 각자 문자열을 조립하면 언젠가 오타 하나로 조용히 어긋나므로 여기에 모읍니다.
 *
 * <p>키를 추가할 때는 반드시 이 클래스에 메서드를 추가하고, 서비스 코드에서
 * 리터럴을 조립하지 않습니다.
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 방 단위 메시지 순번 발번 카운터. {@code INCR} 대상 */
    public static String roomSeq(long roomId) {
        return "room:" + roomId + ":seq";
    }

    /**
     * 방 참여자별 마지막 읽음 seq. Sorted Set (member = userId, score = seq).
     *
     * <p>미읽음 인원 수는 {@code ZCOUNT key -inf (seq} 로 O(log N) 에 구합니다.
     */
    public static String roomRead(long roomId) {
        return "room:" + roomId + ":read";
    }

    /** 방 멤버 캐시 (Set). fanout 대상 조회용. kakao_core 를 직접 읽지 않기 위한 캐시 */
    public static String roomMembers(long roomId) {
        return "room:" + roomId + ":members";
    }

    /**
     * 유저의 활성 커넥션 목록 (Set of "{gatewayId}:{connId}").
     *
     * <p>멀티 디바이스를 지원하므로 Set 입니다. 하트비트마다 TTL 을 갱신하고,
     * 게이트웨이가 죽으면 TTL 로 자동 정리됩니다.
     */
    public static String userConns(long userId) {
        return "user:" + userId + ":conns";
    }

    /** 유저의 채팅방 목록 캐시 (Sorted Set, score = lastMessageAt) */
    public static String userRooms(long userId) {
        return "user:" + userId + ":rooms";
    }

    /** 게이트웨이 노드별 Pub/Sub 채널. Fanout 워커가 이 채널로 릴레이합니다 */
    public static String gatewayChannel(String gatewayId) {
        return "gw." + gatewayId;
    }

    /**
     * 리프레시 토큰 저장소. <b>세션(기기) 단위</b>입니다.
     *
     * <p>유저당 하나로 두면 다른 기기에서 로그인하는 순간 앞 기기가 덮어써지고,
     * 앞 기기가 재발급을 시도할 때 재사용 감지가 양쪽을 다 끊습니다.
     * 세션 id 를 키에 넣으면 각 기기가 자기 것만 회전·무효화합니다.
     */
    public static String refreshToken(long userId, String sessionId) {
        return "user:" + userId + ":refresh:" + sessionId;
    }

    /** 이 유저의 활성 세션 id 목록. 전체 로그아웃과 세션 정리에 씁니다 */
    public static String userSessions(long userId) {
        return "user:" + userId + ":sessions";
    }
}
