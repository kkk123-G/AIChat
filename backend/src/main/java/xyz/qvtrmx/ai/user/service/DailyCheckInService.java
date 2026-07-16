package xyz.qvtrmx.ai.user.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.qvtrmx.ai.common.exception.BusinessException;
import xyz.qvtrmx.ai.user.entity.User;
import xyz.qvtrmx.ai.user.mapper.UserDailyCheckInMapper;
import xyz.qvtrmx.ai.user.mapper.UserMapper;

@Service
public class DailyCheckInService {

    public static final BigDecimal DAILY_CHECK_IN_REWARD = new BigDecimal("5.00");
    private static final ZoneId CHECK_IN_ZONE = ZoneId.of("Asia/Shanghai");

    private final UserDailyCheckInMapper userDailyCheckInMapper;
    private final UserMapper userMapper;
    private final UserBalanceService userBalanceService;

    public DailyCheckInService(
            UserDailyCheckInMapper userDailyCheckInMapper,
            UserMapper userMapper,
            UserBalanceService userBalanceService
    ) {
        this.userDailyCheckInMapper = userDailyCheckInMapper;
        this.userMapper = userMapper;
        this.userBalanceService = userBalanceService;
    }

    public boolean hasCheckedInToday(Long userId) {
        requireActiveUser(userId);
        LocalDate today = LocalDate.now(CHECK_IN_ZONE);
        return userDailyCheckInMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<xyz.qvtrmx.ai.user.entity.UserDailyCheckIn>()
                .eq(xyz.qvtrmx.ai.user.entity.UserDailyCheckIn::getUserId, userId)
                .eq(xyz.qvtrmx.ai.user.entity.UserDailyCheckIn::getCheckInDate, today)) > 0;
    }

    @Transactional
    public void checkIn(Long userId) {
        requireActiveUser(userId);
        LocalDate today = LocalDate.now(CHECK_IN_ZONE);
        Long checkInId = IdWorker.getId();
        if (userDailyCheckInMapper.insertIfAbsent(checkInId, userId, today) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "今日已签到");
        }
        userBalanceService.rewardDailyCheckIn(userId, checkInId);
    }

    private void requireActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1 || user.getStatus() != 1) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号不可用");
        }
    }
}
