package com.kakaoclone.common.jwt;

import com.kakaoclone.common.error.BusinessException;
import com.kakaoclone.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes-long";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final JwtPayload ALICE = new JwtPayload(7L, "alice");

    private JwtProvider providerAt(Instant instant) {
        return new JwtProvider(SECRET, Duration.ofMinutes(30), Duration.ofDays(14),
                Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test
    void 발급한_액세스_토큰을_다시_파싱하면_같은_페이로드가_나온다() {
        JwtProvider provider = providerAt(NOW);

        String token = provider.issueAccessToken(ALICE);

        assertThat(provider.parseAccessToken(token)).isEqualTo(ALICE);
    }

    @Test
    void 만료된_토큰은_TOKEN_EXPIRED_로_거부된다() {
        String token = providerAt(NOW).issueAccessToken(ALICE);
        JwtProvider later = providerAt(NOW.plus(Duration.ofMinutes(31)));

        assertThatThrownBy(() -> later.parseAccessToken(token))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);
    }

    @Test
    void 리프레시_토큰을_액세스_토큰으로_쓸_수_없다() {
        JwtProvider provider = providerAt(NOW);
        String refresh = provider.issueRefreshToken(ALICE);

        assertThatThrownBy(() -> provider.parseAccessToken(refresh))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void 다른_시크릿으로_서명된_토큰은_거부된다() {
        String token = providerAt(NOW).issueAccessToken(ALICE);
        JwtProvider other = new JwtProvider("another-secret-key-that-is-also-32-bytes-long",
                Duration.ofMinutes(30), Duration.ofDays(14), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> other.parseAccessToken(token))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void 위변조된_토큰은_거부된다() {
        JwtProvider provider = providerAt(NOW);
        String token = provider.issueAccessToken(ALICE);
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> provider.parseAccessToken(tampered))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 시크릿이_32바이트_미만이면_생성_자체가_실패한다() {
        assertThatThrownBy(() -> new JwtProvider("too-short", Duration.ofMinutes(30), Duration.ofDays(14)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32");
    }
}
