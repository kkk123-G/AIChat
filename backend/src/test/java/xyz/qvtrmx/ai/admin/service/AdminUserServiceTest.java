package xyz.qvtrmx.ai.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import xyz.qvtrmx.ai.admin.mapper.UserRechargeRecordMapper;
import xyz.qvtrmx.ai.common.exception.BusinessException;
import xyz.qvtrmx.ai.security.model.AuthenticatedUser;
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
    void ordinaryUserCannotListRechargeRecords() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminUserService.listRechargeRecords(1, 10, new AuthenticatedUser(1L, "user", 0))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.status());
        verifyNoInteractions(userMapper, userRechargeRecordMapper, userBalanceService);
    }

    @Test
    void rechargeRecordListRejectsUnsupportedPageSize() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminUserService.listRechargeRecords(1, 15, new AuthenticatedUser(1L, "admin", 1))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verifyNoInteractions(userMapper, userRechargeRecordMapper, userBalanceService);
    }
}
