-- 방 단위 메시지 순번 발번
--
-- KEYS[1] : room:{roomId}:seq
-- ARGV[1] : DB 에 기록된 마지막 seq (Redis 유실 시 복구용, 없으면 0)
--
-- Redis 가 재시작되면 INCR 이 1 부터 다시 시작합니다. seq 가 되돌아가면
-- 클라이언트의 delta sync 가 통째로 깨지므로, "첫 발번일 때만" DB 값에서
-- 이어받아야 합니다. 이 판단과 갱신 사이에 경쟁이 생기므로 Lua 로 원자화합니다.
--
-- 반환: 배정된 seq

local cur = redis.call('INCR', KEYS[1])

if cur == 1 then
    local recovered = tonumber(ARGV[1])
    if recovered and recovered > 0 then
        redis.call('SET', KEYS[1], recovered + 1)
        return recovered + 1
    end
end

return cur
