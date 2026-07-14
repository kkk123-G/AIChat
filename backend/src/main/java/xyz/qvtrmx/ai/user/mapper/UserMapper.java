package xyz.qvtrmx.ai.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import xyz.qvtrmx.ai.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM sys_user WHERE id = #{userId} AND deleted = 0 FOR UPDATE")
    User selectByIdForUpdate(@Param("userId") Long userId);
}
