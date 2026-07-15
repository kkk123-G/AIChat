package xyz.qvtrmx.ai.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.qvtrmx.ai.user.entity.UserDailyCheckIn;

@Mapper
public interface UserDailyCheckInMapper extends BaseMapper<UserDailyCheckIn> {

    @Insert("INSERT IGNORE INTO user_daily_check_in (id, user_id, check_in_date) VALUES (#{id}, #{userId}, #{checkInDate})")
    int insertIfAbsent(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("checkInDate") LocalDate checkInDate
    );
}
