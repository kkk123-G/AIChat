package xyz.qvtrmx.ai.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import xyz.qvtrmx.ai.chat.entity.Conversation;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    @Select("SELECT * FROM ai_conversation WHERE id = #{conversationId} AND user_id = #{userId} "
            + "AND deleted = 0 AND status = 1 FOR UPDATE")
    Conversation selectOwnedForUpdate(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Select("SELECT * FROM ai_conversation c WHERE c.user_id = #{userId} AND c.deleted = 0 "
            + "AND (#{keyword} IS NULL OR c.title LIKE CONCAT('%', #{keyword}, '%') "
            + "OR EXISTS (SELECT 1 FROM ai_chat_message m WHERE m.conversation_id = c.id "
            + "AND m.content LIKE CONCAT('%', #{keyword}, '%'))) "
            + "ORDER BY COALESCE(c.last_message_at, c.created_at) DESC LIMIT #{limit}")
    List<Conversation> searchByUser(@Param("userId") Long userId, @Param("keyword") String keyword,
                                    @Param("limit") int limit);
}
