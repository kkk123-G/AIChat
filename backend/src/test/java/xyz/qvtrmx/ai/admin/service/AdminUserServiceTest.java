package xyz.qvtrmx.ai.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import xyz.qvtrmx.ai.admin.dto.RefundUserRequest;
import xyz.qvtrmx.ai.admin.dto.RechargeUserRequest;
import xyz.qvtrmx.ai.admin.dto.UpdateUserStatusRequest;
import xyz.qvtrmx.ai.admin.entity.UserRechargeRecord;
import xyz.qvtrmx.ai.admin.mapper.UserRechargeRecordMapper;
import xyz.qvtrmx.ai.admin.vo.BalanceAdjustmentResponse;
import xyz.qvtrmx.ai.common.exception.BusinessException;
import xyz.qvtrmx.ai.security.model.AuthenticatedUser;
import xyz.qvtrmx.ai.user.entity.User;
import xyz.qvtrmx.ai.user.mapper.UserMapper;
import xyz.qvtrmx.ai.user.service.UserBalanceService;

class AdminUserServiceTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserRechargeRecordMapper userRechargeRecordMapper = mock(UserRechargeRecordMapper.class);
    private final UserBalanceService userBalanceService = mock(UserBalanceService.class);
    private final AdminUserService adminUserService = new AdminUserService(
            userMapper,
            userRechargeRecordMapper,
            userBalanceService
    );

    @Test
    void ordinaryUserCannotListRechargeRefundRecords() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminUserService.listRechargeRefundRecords(1, 10, new AuthenticatedUser(1L, "user", 0))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.status());
        verifyNoInteractions(userMapper, userRechargeRecordMapper, userBalanceService);
    }

    @Test
    void rechargeRefundRecordListRejectsUnsupportedPageSize() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminUserService.listRechargeRefundRecords(1, 15, new AuthenticatedUser(1L, "admin", 1))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verifyNoInteractions(userMapper, userRechargeRecordMapper, userBalanceService);
    }

    @Test
    void administratorCanDisableAnotherUser() {
        User user = new User();
        user.setId(2L);
        user.setDeleted(0);
        user.setStatus(1);
        when(userMapper.selectById(2L)).thenReturn(user);

        adminUserService.updateUserStatus(2L, new UpdateUserStatusRequest(false), new AuthenticatedUser(1L, "admin", 1));

        assertEquals(0, user.getStatus());
        verify(userMapper).updateById(user);
    }

    @Test
    void administratorCannotDisableSelf() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminUserService.updateUserStatus(1L, new UpdateUserStatusRequest(false), new AuthenticatedUser(1L, "admin", 1))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verifyNoInteractions(userMapper, userRechargeRecordMapper, userBalanceService);
    }

    @Test
    void administratorCanRefundUserBalance() {
        when(userBalanceService.refund(2L, new BigDecimal("8.50"), "退款"))
                .thenReturn(new UserBalanceService.BalanceChange(new BigDecimal("10.00"), new BigDecimal("1.50")));

        BalanceAdjustmentResponse response = adminUserService.refundUser(
                2L,
                new RefundUserRequest(new BigDecimal("8.50"), "退款"),
                new AuthenticatedUser(1L, "admin", 1)
        );

        assertEquals(2L, response.userId());
        assertEquals(new BigDecimal("1.50"), response.balance());
        verify(userBalanceService).refund(2L, new BigDecimal("8.50"), "退款");
        ArgumentCaptor<UserRechargeRecord> recordCaptor = ArgumentCaptor.forClass(UserRechargeRecord.class);
        verify(userRechargeRecordMapper).insert(recordCaptor.capture());
        assertEquals("REFUND", recordCaptor.getValue().getOperationType());
        assertEquals(new BigDecimal("8.50"), recordCaptor.getValue().getAmount());
        assertTrue(recordCaptor.getValue().getRechargeNo().startsWith("RF"));
    }

    @Test
    void administratorRechargeCreatesRechargeOperationRecord() {
        when(userBalanceService.recharge(2L, new BigDecimal("8.50"), "充值"))
                .thenReturn(new UserBalanceService.BalanceChange(new BigDecimal("1.50"), new BigDecimal("10.00")));

        adminUserService.rechargeUser(
                2L,
                new RechargeUserRequest(new BigDecimal("8.50"), "充值"),
                new AuthenticatedUser(1L, "admin", 1)
        );

        ArgumentCaptor<UserRechargeRecord> recordCaptor = ArgumentCaptor.forClass(UserRechargeRecord.class);
        verify(userRechargeRecordMapper).insert(recordCaptor.capture());
        assertEquals("RECHARGE", recordCaptor.getValue().getOperationType());
        assertTrue(recordCaptor.getValue().getRechargeNo().startsWith("RC"));
    }
}
