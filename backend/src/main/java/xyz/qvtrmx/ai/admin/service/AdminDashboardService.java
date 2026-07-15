package xyz.qvtrmx.ai.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import xyz.qvtrmx.ai.admin.vo.AdminDashboardResponse;
import xyz.qvtrmx.ai.admin.vo.DashboardTopUserResponse;
import xyz.qvtrmx.ai.admin.vo.DashboardTrendResponse;
import xyz.qvtrmx.ai.chat.entity.ChatMessage;
import xyz.qvtrmx.ai.chat.mapper.ChatMessageMapper;
import xyz.qvtrmx.ai.chat.model.HourlyUsage;
import xyz.qvtrmx.ai.user.entity.User;
import xyz.qvtrmx.ai.user.mapper.UserMapper;

@Service
public class AdminDashboardService {

    private static final String USER_MESSAGE_ROLE = "user";
    private static final String ASSISTANT_MESSAGE_ROLE = "assistant";
    private static final ZoneId DASHBOARD_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter HOUR_KEY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH");
    private static final DateTimeFormatter HOUR_LABEL_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final UserMapper userMapper;
    private final ChatMessageMapper chatMessageMapper;

    public AdminDashboardService(UserMapper userMapper, ChatMessageMapper chatMessageMapper) {
        this.userMapper = userMapper;
        this.chatMessageMapper = chatMessageMapper;
    }

    public AdminDashboardResponse overview() {
        LocalDateTime now = LocalDateTime.now(DASHBOARD_ZONE);
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime firstHour = now.withMinute(0).withSecond(0).withNano(0).minusHours(23);

        long userCount = countUsers(null);
        long newUsersToday = countUsers(startOfToday);
        long totalRequests = countMessages(USER_MESSAGE_ROLE, null);
        long todayRequests = countMessages(USER_MESSAGE_ROLE, startOfToday);
        long totalSuccesses = countMessages(ASSISTANT_MESSAGE_ROLE, null);
        long todaySuccesses = countMessages(ASSISTANT_MESSAGE_ROLE, startOfToday);
        long todayFailures = Math.max(0, todayRequests - todaySuccesses);
        long totalFailures = Math.max(0, totalRequests - totalSuccesses);

        List<String> hourKeys = new ArrayList<>();
        List<String> hourLabels = new ArrayList<>();
        for (int offset = 0; offset < 24; offset++) {
            LocalDateTime hour = firstHour.plusHours(offset);
            hourKeys.add(hour.format(HOUR_KEY_FORMAT));
            hourLabels.add(hour.format(HOUR_LABEL_FORMAT));
        }

        List<HourlyUsage> allUsage = chatMessageMapper.selectHourlyUsage(firstHour, null);
        DashboardTrendResponse requestTrend = new DashboardTrendResponse(hourLabels, aggregateUsage(allUsage, hourKeys));

        List<Long> topUserIds = chatMessageMapper.selectTopUserIds(now.minusHours(24));
        List<DashboardTopUserResponse> topUsers = topUserIds.isEmpty()
                ? List.of()
                : topUsers(firstHour, topUserIds, hourKeys);

        return new AdminDashboardResponse(
                userCount,
                newUsersToday,
                todayRequests,
                totalRequests,
                todaySuccesses,
                totalSuccesses,
                todayFailures,
                totalFailures,
                percentage(todaySuccesses, todayRequests),
                percentage(totalSuccesses, totalRequests),
                percentage(todayFailures, todayRequests),
                percentage(totalFailures, totalRequests),
                requestTrend,
                topUsers
        );
    }

    private long countUsers(LocalDateTime from) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>().eq(User::getDeleted, 0);
        if (from != null) {
            query.ge(User::getCreatedAt, from);
        }
        return userMapper.selectCount(query);
    }

    private long countMessages(String role, LocalDateTime from) {
        LambdaQueryWrapper<ChatMessage> query = new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getRole, role);
        if (from != null) {
            query.ge(ChatMessage::getCreatedAt, from);
        }
        return chatMessageMapper.selectCount(query);
    }

    private List<DashboardTopUserResponse> topUsers(
            LocalDateTime firstHour,
            List<Long> topUserIds,
            List<String> hourKeys
    ) {
        Map<Long, User> usersById = userMapper.selectByIds(topUserIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Map<String, Long>> usageByUserAndHour = indexUsage(chatMessageMapper.selectHourlyUsage(firstHour, topUserIds));
        return topUserIds.stream()
                .map(userId -> {
                    User user = usersById.get(userId);
                    String username = user == null ? "User #" + userId : user.getUsername();
                    return new DashboardTopUserResponse(
                            username,
                            valuesFor(usageByUserAndHour.getOrDefault(userId, Map.of()), hourKeys)
                    );
                })
                .toList();
    }

    private List<Long> aggregateUsage(List<HourlyUsage> usage, List<String> hourKeys) {
        Map<String, Long> totalsByHour = new HashMap<>();
        for (HourlyUsage item : usage) {
            totalsByHour.merge(item.getHourKey(), item.getRequestCount(), Long::sum);
        }
        return valuesFor(totalsByHour, hourKeys);
    }

    private Map<Long, Map<String, Long>> indexUsage(List<HourlyUsage> usage) {
        Map<Long, Map<String, Long>> indexed = new HashMap<>();
        for (HourlyUsage item : usage) {
            indexed.computeIfAbsent(item.getUserId(), ignored -> new HashMap<>())
                    .merge(item.getHourKey(), item.getRequestCount(), Long::sum);
        }
        return indexed;
    }

    private List<Long> valuesFor(Map<String, Long> valuesByHour, List<String> hourKeys) {
        return hourKeys.stream().map(hourKey -> valuesByHour.getOrDefault(hourKey, 0L)).toList();
    }

    private BigDecimal percentage(long count, long total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
}
