package xyz.qvtrmx.ai.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("user_recharge_record")
@Getter
@Setter
public class UserRechargeRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String rechargeNo;
    private Long userId;
    private Long operatorId;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String remark;
    private LocalDateTime createdAt;
}
