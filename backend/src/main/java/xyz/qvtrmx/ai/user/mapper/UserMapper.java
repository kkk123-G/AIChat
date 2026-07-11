package xyz.qvtrmx.ai.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import xyz.qvtrmx.ai.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
