package xyz.qvtrmx.ai.user.service;

import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.qvtrmx.ai.common.exception.BusinessException;
import xyz.qvtrmx.ai.user.entity.User;
import xyz.qvtrmx.ai.user.entity.UserBalanceChangeRecord;
import xyz.qvtrmx.ai.user.mapper.UserBalanceChangeRecordMapper;
import xyz.qvtrmx.ai.user.mapper.UserMapper;

@Service
public class UserBalanceService {

    public static final BigDecimal AI_CHAT_FEE = new BigDecimal("1.00");
    private static final int ENABLED_STATUS = 1;

    private final UserMapper userMapper;
    private final UserBalanceChangeRecordMapper userBalanceChangeRecordMapper;

    public UserBalanceService(UserMapper userMapper, UserBalanceChangeRecordMapper userBalanceChangeRecordMapper) {
        this.userMapper = userMapper;
        this.userBalanceChangeRecordMapper = userBalanceChangeRecordMapper;
    }

    @Transactional
    public BalanceChange recharge(Long userId, BigDecimal amount, String remark) {
        return changeBalance(userId, amount, "RECHARGE", null, remark, true);
    }

    @Transactional
    public BalanceChange chargeAiChat(Long userId, Long userMessageId) {
        return changeBalance(userId, AI_CHAT_FEE.negate(), "AI_CHAT", userMessageId, "AI chat response charge", true);
    }

    @Transactional
    public BalanceChange refundAiChat(Long userId, Long userMessageId) {
        return changeBalance(userId, AI_CHAT_FEE, "AI_CHAT_REFUND", userMessageId, "AI chat response refund", false);
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

    public record BalanceChange(BigDecimal balanceBefore, BigDecimal balanceAfter) {
    }
}
