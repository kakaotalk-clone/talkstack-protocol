package com.kakaoclone.common.jwt;

import com.kakaoclone.common.error.BusinessException;
import com.kakaoclone.common.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;

/**
 * 액세스/리프레시 토큰 발급 · 검증.
 *
 * <p>API 는 발급과 검증을, Gateway 는 검증만 합니다. 두 서비스가 같은 클래스를
 * 쓰기 때문에 서명 방식이 어긋날 일이 없습니다 — 멀티레포에서 common 모듈을
 * 두는 이유가 이겁니다.
 *
 * <p>Spring 에 의존하지 않습니다. 각 서비스가 {@code @Bean} 으로 감쌉니다.
 */
public class JwtProvider {

    private static final String CLAIM_LOGIN_ID = "lid";
    private static final String CLAIM_TYPE = "typ";

    /**
     * 세션(기기) 식별자. <b>리프레시 토큰에만</b> 들어갑니다.
     *
     * <p>이게 없으면 저장소가 유저당 토큰 하나만 들 수 있어, 다른 기기에서
     * 로그인하는 순간 앞 기기의 토큰이 덮어써집니다. 그 뒤 앞 기기가 재발급을
     * 시도하면 재사용 감지가 발동해 <b>양쪽 세션이 모두</b> 끊깁니다.
     *
     * <p>클라이언트가 만들어 보내지 않고 서버가 로그인 시 발급합니다 —
     * 서명된 토큰 안에 있으므로 위조할 수 없고, 클라이언트는 따로 보관할 것도 없습니다.
     */
    private static final String CLAIM_SESSION_ID = "sid";
    private static final String TYPE_ACCESS = "a";
    private static final String TYPE_REFRESH = "r";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final Clock clock;

    public JwtProvider(String secret, Duration accessTtl, Duration refreshTtl) {
        this(secret, accessTtl, refreshTtl, Clock.systemUTC());
    }

    /** 테스트에서 만료를 검증하려면 고정 Clock 을 주입합니다. */
    public JwtProvider(String secret, Duration accessTtl, Duration refreshTtl, Clock clock) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT secret 은 최소 32바이트여야 합니다 (HS256)");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
        this.clock = clock;
    }

    public String issueAccessToken(JwtPayload payload) {
        return issue(payload, TYPE_ACCESS, accessTtl);
    }

    public String issueRefreshToken(JwtPayload payload) {
        return issue(payload, TYPE_REFRESH, refreshTtl);
    }

    /**
     * 액세스 토큰을 검증하고 페이로드를 돌려줍니다.
     *
     * @throws BusinessException {@link ErrorCode#TOKEN_EXPIRED} 또는
     *                           {@link ErrorCode#INVALID_TOKEN}
     */
    public JwtPayload parseAccessToken(String token) {
        return parse(token, TYPE_ACCESS);
    }

    public JwtPayload parseRefreshToken(String token) {
        return parse(token, TYPE_REFRESH);
    }

    private String issue(JwtPayload payload, String type, Duration ttl) {
        Date now = Date.from(clock.instant());
        Date expiry = Date.from(clock.instant().plus(ttl));
        var builder = Jwts.builder()
                .subject(String.valueOf(payload.userId()))
                .claim(CLAIM_LOGIN_ID, payload.loginId())
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(expiry);

        if (payload.sessionId() != null) {
            builder.claim(CLAIM_SESSION_ID, payload.sessionId());
        }
        return builder.signWith(key).compact();
    }

    private JwtPayload parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN, "토큰 종류가 올바르지 않습니다");
            }
            return new JwtPayload(
                    Long.parseLong(claims.getSubject()),
                    claims.get(CLAIM_LOGIN_ID, String.class),
                    claims.get(CLAIM_SESSION_ID, String.class));

        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }
}
