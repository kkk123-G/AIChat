package xyz.qvtrmx.ai.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("user_balance_change_record")
@Getter
@Setter
public class UserBalanceChangeRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private BigDecimal changeAmount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String changeType;
    private Long referenceId;
    private String remark;
    private LocalDateTime createdAt;
}
