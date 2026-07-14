package xyz.qvtrmx.ai.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.qvtrmx.ai.chat.entity.ChatMessage;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
