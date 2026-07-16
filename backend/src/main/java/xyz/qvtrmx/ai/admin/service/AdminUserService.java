package xyz.qvtrmx.ai.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import xyz.qvtrmx.ai.admin.dto.RechargeUserRequest;
import xyz.qvtrmx.ai.admin.dto.RefundUserRequest;
import xyz.qvtrmx.ai.admin.dto.UpdateUserStatusRequest;
import xyz.qvtrmx.ai.admin.entity.UserRechargeRecord;
import xyz.qvtrmx.ai.admin.mapper.UserRechargeRecordMapper;
import xyz.qvtrmx.ai.admin.vo.AdminUserResponse;
import xyz.qvtrmx.ai.admin.vo.BalanceAdjustmentResponse;
import xyz.qvtrmx.ai.admin.vo.RechargeRefundRecordResponse;
import xyz.qvtrmx.ai.admin.vo.RechargeUserResponse;
import xyz.qvtrmx.ai.common.api.PageResponse;
import xyz.qvtrmx.ai.common.exception.BusinessException;
import xyz.qvtrmx.ai.security.model.UserRole;
import xyz.qvtrmx.ai.security.model.AuthenticatedUser;
import xyz.qvtrmx.ai.user.entity.User;
import xyz.qvtrmx.ai.user.mapper.UserMapper;
import xyz.qvtrmx.ai.user.service.UserBalanceService;

@Service
public class AdminUserService {

    private static final List<Long> ALLOWED_PAGE_SIZES = List.of(10L, 20L, 50L);
    private static final int ENABLED_STATUS = 1;

    private final UserMapper userMapper;
    private final UserRechargeRecordMapper userRechargeRecordMapper;
    private final UserBalanceService userBalanceService;

    public AdminUserService(
            UserMapper userMapper,
            UserRechargeRecordMapper userRechargeRecordMapper,
            UserBalanceService userBalanceService
    ) {
        this.userMapper = userMapper;
        this.userRechargeRecordMapper = userRechargeRecordMapper;
        this.userBalanceService = userBalanceService;
    }

    public PageResponse<AdminUserResponse> listUsers(long pageNumber, long pageSize, String keyword) {
        validatePageArguments(pageNumber, pageSize);

        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreatedAt);
        if (StringUtils.hasText(keyword)) {
            query.like(User::getUsername, keyword.trim());
        }

        Page<User> page = userMapper.selectPage(new Page<>(pageNumber, pageSize), query);
        List<AdminUserResponse> records = page.getRecords().stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public PageResponse<RechargeRefundRecordResponse> listRechargeRefundRecords(
            long pageNumber,
            long pageSize,
            AuthenticatedUser operator
    ) {
        requireAdministrator(operator);
        validatePageArguments(pageNumber, pageSize);

        Page<UserRechargeRecord> page = userRechargeRecordMapper.selectPage(
                new Page<>(pageNumber, pageSize),
                new LambdaQueryWrapper<UserRechargeRecord>()
                        .orderByDesc(UserRechargeRecord::getCreatedAt)
                        .orderByDesc(UserRechargeRecord::getId)
        );
        Map<Long, User> usersById = loadUsersById(page.getRecords());
        List<RechargeRefundRecordResponse> records = page.getRecords().stream()
                .map(record -> toRechargeRefundRecordResponse(record, usersById))
                .toList();
        return new PageResponse<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @org.springframework.transaction.annotation.Transactional
    public RechargeUserResponse rechargeUser(Long userId, RechargeUserRequest request, AuthenticatedUser operator) {
        requireAdministrator(operator);

        String remark = StringUtils.hasText(request.remark()) ? request.remark().trim() : null;
        UserBalanceService.BalanceChange balanceChange = userBalanceService.recharge(userId, request.amount(), remark);

        createOperationRecord(userId, operator.id(), request.amount(), balanceChange, remark, "RECHARGE", "RC");
        return new RechargeUserResponse(userId, balanceChange.balanceAfter());
    }

    @org.springframework.transaction.annotation.Transactional
    public BalanceAdjustmentResponse refundUser(Long userId, RefundUserRequest request, AuthenticatedUser operator) {
        requireAdministrator(operator);

        String remark = StringUtils.hasText(request.remark()) ? request.remark().trim() : null;
        UserBalanceService.BalanceChange balanceChange = userBalanceService.refund(userId, request.amount(), remark);
        createOperationRecord(userId, operator.id(), request.amount(), balanceChange, remark, "REFUND", "RF");
        return new BalanceAdjustmentResponse(userId, balanceChange.balanceAfter());
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateUserStatus(Long userId, UpdateUserStatusRequest request, AuthenticatedUser operator) {
        requireAdministrator(operator);
        if (operator.id().equals(userId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "You cannot change your own account status");
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "User was not found");
        }
        user.setStatus(request.enabled() ? ENABLED_STATUS : 0);
        userMapper.updateById(user);
    }

    private void validatePageArguments(long pageNumber, long pageSize) {
        if (pageNumber < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Page number must be greater than zero");
        }
        if (!ALLOWED_PAGE_SIZES.contains(pageSize)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Page size must be one of: 10, 20, 50");
        }
    }

    private void requireAdministrator(AuthenticatedUser operator) {
        if (operator == null || operator.role() != UserRole.ADMIN.code()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Administrator permission is required");
        }
    }

    private Map<Long, User> loadUsersById(List<UserRechargeRecord> records) {
        Set<Long> userIds = new HashSet<>();
        for (UserRechargeRecord record : records) {
            userIds.add(record.getUserId());
            userIds.add(record.getOperatorId());
        }
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private void createOperationRecord(
            Long userId,
            Long operatorId,
            BigDecimal amount,
            UserBalanceService.BalanceChange balanceChange,
            String remark,
            String operationType,
            String numberPrefix
    ) {
        UserRechargeRecord record = new UserRechargeRecord();
        record.setId(IdWorker.getId());
        record.setRechargeNo(numberPrefix + record.getId());
        record.setOperationType(operationType);
        record.setUserId(userId);
        record.setOperatorId(operatorId);
        record.setAmount(amount);
        record.setBalanceBefore(balanceChange.balanceBefore());
        record.setBalanceAfter(balanceChange.balanceAfter());
        record.setRemark(remark);
        userRechargeRecordMapper.insert(record);
    }

    private RechargeRefundRecordResponse toRechargeRefundRecordResponse(UserRechargeRecord record, Map<Long, User> usersById) {
        User recipient = usersById.get(record.getUserId());
        User operator = usersById.get(record.getOperatorId());
        return new RechargeRefundRecordResponse(
                String.valueOf(record.getId()),
                record.getRechargeNo(),
                recipient == null ? "Deleted user" : recipient.getUsername(),
                record.getAmount(),
                record.getOperationType(),
                record.getBalanceBefore(),
                record.getBalanceAfter(),
                record.getRemark(),
                operator == null ? "Deleted administrator" : operator.getUsername(),
                record.getCreatedAt()
        );
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                String.valueOf(user.getId()),
                user.getUsername(),
                UserRole.fromCode(user.getRole()).name(),
                user.getBalance(),
                user.getStatus() == ENABLED_STATUS ? "ENABLED" : "DISABLED",
                user.getLastActiveAt(),
                user.getLastUsedAt(),
                user.getCreatedAt()
        );
    }
}
