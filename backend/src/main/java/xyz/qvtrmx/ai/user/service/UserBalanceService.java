package xyz.qvtrmx.ai.user.service;

import java.math.BigDecimal;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.qvtrmx.ai.common.api.PageResponse;
import xyz.qvtrmx.ai.common.exception.BusinessException;
import xyz.qvtrmx.ai.security.model.AuthenticatedUser;
import xyz.qvtrmx.ai.user.entity.User;
import xyz.qvtrmx.ai.user.entity.UserBalanceChangeRecord;
import xyz.qvtrmx.ai.user.mapper.UserBalanceChangeRecordMapper;
import xyz.qvtrmx.ai.user.mapper.UserMapper;
import xyz.qvtrmx.ai.user.vo.BalanceChangeRecordResponse;

@Service
public class UserBalanceService {

    public static final BigDecimal AI_CHAT_FEE = new BigDecimal("1.00");
    private static final int ENABLED_STATUS = 1;
    private static final List<Long> ALLOWED_PAGE_SIZES = List.of(10L, 20L, 50L);

    private final UserMapper userMapper;
    private final UserBalanceChangeRecordMapper userBalanceChangeRecordMapper;

    public UserBalanceService(UserMapper userMapper, UserBalanceChangeRecordMapper userBalanceChangeRecordMapper) {
        this.userMapper = userMapper;
        this.userBalanceChangeRecordMapper = userBalanceChangeRecordMapper;
    }

    @Transactional
    public BalanceChange recharge(Long userId, BigDecimal amount, String remark) {
        return changeBalance(userId, amount, "ADMIN_RECHARGE", null, remark, true);
    }

    @Transactional
    public BalanceChange refund(Long userId, BigDecimal amount, String remark) {
        return changeBalance(userId, amount.negate(), "ADMIN_REFUND", null, remark, false);
    }

    @Transactional
    public BalanceChange chargeAiChat(Long userId, Long userMessageId) {
        return changeBalance(userId, AI_CHAT_FEE.negate(), "AI_CHAT_CONSUMPTION", userMessageId, "AI聊天消耗了1元", true);
    }

    @Transactional
    public BalanceChange refundAiChat(Long userId, Long userMessageId) {
        return changeBalance(userId, AI_CHAT_FEE, "AI_CHAT_REFUND", userMessageId, "AI聊天请求失败退还1元", false);
    }

    @Transactional
    public BalanceChange rewardDailyCheckIn(Long userId, Long checkInId) {
        return changeBalance(userId, DailyCheckInService.DAILY_CHECK_IN_REWARD, "DAILY_CHECK_IN", checkInId, "签到成功获得5元", true);
    }

    public PageResponse<BalanceChangeRecordResponse> listBalanceChanges(
            long pageNumber,
            long pageSize,
            AuthenticatedUser authenticatedUser
    ) {
        validatePageArguments(pageNumber, pageSize);
        requireActiveUser(authenticatedUser.id());

        Page<UserBalanceChangeRecord> page = userBalanceChangeRecordMapper.selectPage(
                new Page<>(pageNumber, pageSize),
                new LambdaQueryWrapper<UserBalanceChangeRecord>()
                        .eq(UserBalanceChangeRecord::getUserId, authenticatedUser.id())
                        .orderByDesc(UserBalanceChangeRecord::getCreatedAt)
                        .orderByDesc(UserBalanceChangeRecord::getId)
        );
        List<BalanceChangeRecordResponse> records = page.getRecords().stream()
                .map(this::toBalanceChangeRecordResponse)
                .toList();
        return new PageResponse<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    private BalanceChange changeBalance(
            Long userId,
            BigDecimal changeAmount,
            String changeType,
            Long referenceId,
            String remark,
            boolean requireEnabled
    ) {
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null || (requireEnabled && user.getStatus() != ENABLED_STATUS)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "User was not found or is disabled");
        }

        BigDecimal balanceBefore = user.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(changeAmount);
        if (balanceAfter.signum() < 0) {
            throw new BusinessException(HttpStatus.PAYMENT_REQUIRED, "余额不足");
        }

        user.setBalance(balanceAfter);
        userMapper.updateById(user);

        UserBalanceChangeRecord record = new UserBalanceChangeRecord();
        record.setUserId(userId);
        record.setChangeAmount(changeAmount);
        record.setBalanceBefore(balanceBefore);
        record.setBalanceAfter(balanceAfter);
        record.setChangeType(changeType);
        record.setReferenceId(referenceId);
        record.setRemark(remark);
        userBalanceChangeRecordMapper.insert(record);
        return new BalanceChange(balanceBefore, balanceAfter);
    }

    private void validatePageArguments(long pageNumber, long pageSize) {
        if (pageNumber < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Page number must be greater than zero");
        }
        if (!ALLOWED_PAGE_SIZES.contains(pageSize)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Page size must be one of: 10, 20, 50");
        }
    }

    private void requireActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1 || user.getStatus() != ENABLED_STATUS) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Account is unavailable");
        }
    }

    private BalanceChangeRecordResponse toBalanceChangeRecordResponse(UserBalanceChangeRecord record) {
        return new BalanceChangeRecordResponse(
                String.valueOf(record.getId()),
                record.getChangeAmount(),
                record.getCreatedAt(),
                displayType(record.getChangeType()),
                record.getRemark()
        );
    }

    private String displayType(String changeType) {
        return switch (changeType) {
            case "ADMIN_RECHARGE", "RECHARGE" -> "管理员充值";
            case "ADMIN_REFUND" -> "管理员退款";
            case "DAILY_CHECK_IN" -> "签到奖励";
            case "AI_CHAT", "AI_CHAT_CONSUMPTION" -> "AI聊天消耗";
            case "AI_CHAT_REFUND" -> "AI聊天退款";
            default -> "其他";
        };
    }

    public record BalanceChange(BigDecimal balanceBefore, BigDecimal balanceAfter) {
    }
}
