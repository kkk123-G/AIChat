package xyz.qvtrmx.ai.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import xyz.qvtrmx.ai.common.exception.BusinessException;
import xyz.qvtrmx.ai.user.entity.User;
import xyz.qvtrmx.ai.user.mapper.UserDailyCheckInMapper;
import xyz.qvtrmx.ai.user.mapper.UserMapper;

class DailyCheckInServiceTest {

    private final UserDailyCheckInMapper userDailyCheckInMapper = mock(UserDailyCheckInMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserBalanceService userBalanceService = mock(UserBalanceService.class);
    private final DailyCheckInService dailyCheckInService = new DailyCheckInService(
            userDailyCheckInMapper,
            userMapper,
            userBalanceService
    );

    @Test
    void checkInRewardsActiveUser() {
        when(userMapper.selectById(1L)).thenReturn(activeUser());
        when(userDailyCheckInMapper.insertIfAbsent(anyLong(), eq(1L), eq(LocalDate.now()))).thenReturn(1);

        dailyCheckInService.checkIn(1L);

        verify(userBalanceService).rewardDailyCheckIn(eq(1L), anyLong());
    }

    @Test
    void duplicateCheckInDoesNotRewardAgain() {
        when(userMapper.selectById(1L)).thenReturn(activeUser());
        when(userDailyCheckInMapper.insertIfAbsent(anyLong(), eq(1L), eq(LocalDate.now()))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> dailyCheckInService.checkIn(1L));

        assertEquals(HttpStatus.CONFLICT, exception.status());
        verifyNoInteractions(userBalanceService);
    }

    @Test
    void disabledUserCannotCheckIn() {
        User user = activeUser();
        user.setStatus(0);
        when(userMapper.selectById(1L)).thenReturn(user);

        BusinessException exception = assertThrows(BusinessException.class, () -> dailyCheckInService.checkIn(1L));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.status());
        verifyNoInteractions(userDailyCheckInMapper, userBalanceService);
    }

    private User activeUser() {
        User user = new User();
        user.setId(1L);
        user.setStatus(1);
        user.setDeleted(0);
        return user;
    }
}
