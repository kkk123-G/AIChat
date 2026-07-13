package xyz.qvtrmx.ai.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("user_refresh_token")
@Getter
@Setter
public class RefreshTokenSession {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String tokenId;
    private String tokenHash;
    private String deviceId;
    private String userAgent;
    private String clientIp;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime revokedAt;
}
