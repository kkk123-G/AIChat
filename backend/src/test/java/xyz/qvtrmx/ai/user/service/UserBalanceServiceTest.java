package xyz.qvtrmx.ai.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import xyz.qvtrmx.ai.common.api.PageResponse;
import xyz.qvtrmx.ai.common.exception.BusinessException;
import xyz.qvtrmx.ai.security.model.AuthenticatedUser;
import xyz.qvtrmx.ai.user.entity.User;
import xyz.qvtrmx.ai.user.entity.UserBalanceChangeRecord;
import xyz.qvtrmx.ai.user.mapper.UserBalanceChangeRecordMapper;
import xyz.qvtrmx.ai.user.mapper.UserMapper;
import xyz.qvtrmx.ai.user.vo.BalanceChangeRecordResponse;

class UserBalanceServiceTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserBalanceChangeRecordMapper userBalanceChangeRecordMapper = mock(UserBalanceChangeRecordMapper.class);
    private final UserBalanceService userBalanceService = new UserBalanceService(userMapper, userBalanceChangeRecordMapper);

    @Test
    void listsOnlyTheAuthenticatedUsersBalanceChanges() {
        when(userMapper.selectById(7L)).thenReturn(activeUser());
        when(userBalanceChangeRecordMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<UserBalanceChangeRecord> page = invocation.getArgument(0);
            page.setRecords(java.util.List.of(balanceChangeRecord()));
            page.setTotal(1);
            return page;
        });

        PageResponse<BalanceChangeRecordResponse> result = userBalanceService.listBalanceChanges(
                1,
                10,
                new AuthenticatedUser(7L, "user", 0)
        );

        assertEquals(1, result.total());
        assertEquals("管理员充值", result.records().getFirst().type());
        assertEquals(new BigDecimal("20.00"), result.records().getFirst().amount());
    }

    @Test
    void rejectsUnsupportedBalanceChangePageSize() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userBalanceService.listBalanceChanges(1, 15, new AuthenticatedUser(7L, "user", 0))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verifyNoInteractions(userMapper, userBalanceChangeRecordMapper);
    }

    private User activeUser() {
        User user = new User();
        user.setId(7L);
        user.setDeleted(0);
        user.setStatus(1);
        return user;
    }

    private UserBalanceChangeRecord balanceChangeRecord() {
        UserBalanceChangeRecord record = new UserBalanceChangeRecord();
        record.setId(1L);
        record.setUserId(7L);
        record.setChangeAmount(new BigDecimal("20.00"));
        record.setChangeType("ADMIN_RECHARGE");
        record.setRemark("管理员充值");
        record.setCreatedAt(LocalDateTime.of(2026, 7, 15, 16, 0));
        return record;
    }
}
