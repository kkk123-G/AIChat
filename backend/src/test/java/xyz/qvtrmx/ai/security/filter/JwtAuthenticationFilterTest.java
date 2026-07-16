package xyz.qvtrmx.ai.security.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import xyz.qvtrmx.ai.security.jwt.JwtService;
import xyz.qvtrmx.ai.user.mapper.UserMapper;

import static org.mockito.Mockito.mock;

class JwtAuthenticationFilterTest {

    @Test
    void authenticatesAsyncDispatches() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                new JwtService("01234567890123456789012345678901", Duration.ofMinutes(15)),
                mock(UserMapper.class)
        );

        assertFalse(filter.shouldNotFilterAsyncDispatch());
    }
}
