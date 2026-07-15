package xyz.qvtrmx.ai.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.Test;
import xyz.qvtrmx.ai.admin.vo.AdminDashboardResponse;
import xyz.qvtrmx.ai.chat.mapper.ChatMessageMapper;
import xyz.qvtrmx.ai.chat.model.HourlyUsage;
import xyz.qvtrmx.ai.user.entity.User;
import xyz.qvtrmx.ai.user.mapper.UserMapper;

class AdminDashboardServiceTest {

    private static final DateTimeFormatter HOUR_KEY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH");
    private final UserMapper userMapper = mock(UserMapper.class);
    private final ChatMessageMapper chatMessageMapper = mock(ChatMessageMapper.class);
    private final AdminDashboardService adminDashboardService = new AdminDashboardService(userMapper, chatMessageMapper);

    @Test
    void returnsTopUsersAsSeparateTwentyFourHourSeries() {
        String firstHour = LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .minusHours(23)
                .format(HOUR_KEY_FORMAT);
        when(userMapper.selectCount(any())).thenReturn(8L, 2L);
        when(chatMessageMapper.selectCount(any())).thenReturn(100L, 10L, 90L, 9L);
        when(chatMessageMapper.selectTopUserIds(any())).thenReturn(List.of(7L, 8L));
        when(userMapper.selectByIds(List.of(7L, 8L))).thenReturn(List.of(user(7L, "alice"), user(8L, "bob")));
        when(chatMessageMapper.selectHourlyUsage(any(), isNull())).thenReturn(List.of(
                hourlyUsage(7L, firstHour, 3L),
                hourlyUsage(8L, firstHour, 2L)
        ));
        when(chatMessageMapper.selectHourlyUsage(any(), eq(List.of(7L, 8L)))).thenReturn(List.of(
                hourlyUsage(7L, firstHour, 3L),
                hourlyUsage(8L, firstHour, 2L)
        ));

        AdminDashboardResponse result = adminDashboardService.overview();

        assertEquals(24, result.requestTrend().labels().size());
        assertEquals(24, result.topUsers().get(0).values().size());
        assertEquals("alice", result.topUsers().get(0).username());
        assertEquals("bob", result.topUsers().get(1).username());
        assertEquals(3L, result.topUsers().get(0).values().getFirst());
        assertEquals(2L, result.topUsers().get(1).values().getFirst());
        assertEquals(5L, result.requestTrend().values().getFirst());
        assertEquals(1L, result.todayFailures());
        assertEquals(10L, result.totalFailures());
        assertEquals(new BigDecimal("90.0"), result.todaySuccessRate());
        assertEquals(new BigDecimal("90.0"), result.totalSuccessRate());
        assertEquals(new BigDecimal("10.0"), result.todayFailureRate());
        assertEquals(new BigDecimal("10.0"), result.totalFailureRate());
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private HourlyUsage hourlyUsage(Long userId, String hourKey, Long requestCount) {
        HourlyUsage usage = new HourlyUsage();
        usage.setUserId(userId);
        usage.setHourKey(hourKey);
        usage.setRequestCount(requestCount);
        return usage;
    }
}
