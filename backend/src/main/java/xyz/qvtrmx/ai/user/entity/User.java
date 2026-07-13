package xyz.qvtrmx.ai.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("sys_user")
@Getter
@Setter
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private BigDecimal balance;
    private Integer role;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime lastActiveAt;
    private LocalDateTime lastUsedAt;
    private Integer version;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
