package xyz.qvtrmx.ai.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import xyz.qvtrmx.ai.auth.entity.RefreshTokenSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RefreshTokenSessionMapper extends BaseMapper<RefreshTokenSession> {
}
