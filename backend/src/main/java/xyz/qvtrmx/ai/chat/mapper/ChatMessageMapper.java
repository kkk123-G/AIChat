package xyz.qvtrmx.ai.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;
import xyz.qvtrmx.ai.chat.entity.ChatMessage;
import xyz.qvtrmx.ai.chat.model.HourlyUsage;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("""
            SELECT user_id
            FROM ai_chat_message
            WHERE role = 'user' AND created_at >= #{from}
            GROUP BY user_id
            ORDER BY COUNT(*) DESC, user_id ASC
            LIMIT 5
            """)
    List<Long> selectTopUserIds(@Param("from") LocalDateTime from);

    @Select("""
            <script>
            SELECT user_id, DATE_FORMAT(created_at, '%Y-%m-%d %H') AS hour_key, COUNT(*) AS request_count
            FROM ai_chat_message
            WHERE role = 'user' AND created_at >= #{from}
            <if test="userIds != null and !userIds.isEmpty()">
              AND user_id IN
              <foreach collection="userIds" item="userId" open="(" separator="," close=")">
                #{userId}
              </foreach>
            </if>
            GROUP BY user_id, DATE_FORMAT(created_at, '%Y-%m-%d %H')
            ORDER BY hour_key ASC
            </script>
            """)
    List<HourlyUsage> selectHourlyUsage(
            @Param("from") LocalDateTime from,
            @Param("userIds") List<Long> userIds
    );
}
