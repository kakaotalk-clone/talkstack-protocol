-- 읽음 위치 갱신 (뒤로 가지 않음)
--
-- KEYS[1] : room:{roomId}:read   (Sorted Set: member=userId, score=seq)
-- ARGV[1] : userId
-- ARGV[2] : seq
--
-- 모바일 네트워크에서는 읽음 이벤트가 순서가 뒤바뀌어 도착합니다.
-- 단순 ZADD 로 짜면 읽음 표시가 되돌아가는 버그가 납니다.
-- 기존 값보다 클 때만 갱신합니다. (Redis 6.2+ 의 ZADD GT 와 동일한 동작)
--
-- 반환: 1 = 갱신됨, 0 = 무시됨(오래된 이벤트)

local cur = redis.call('ZSCORE', KEYS[1], ARGV[1])

if cur == false or tonumber(cur) < tonumber(ARGV[2]) then
    redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])
    return 1
end

return 0
