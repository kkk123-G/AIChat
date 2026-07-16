package xyz.qvtrmx.ai.common.audit;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xyz.qvtrmx.ai.admin.dto.RechargeUserRequest;
import xyz.qvtrmx.ai.admin.dto.RefundUserRequest;
import xyz.qvtrmx.ai.admin.dto.UpdateUserStatusRequest;
import xyz.qvtrmx.ai.admin.vo.BalanceAdjustmentResponse;
import xyz.qvtrmx.ai.admin.vo.RechargeUserResponse;
import xyz.qvtrmx.ai.auth.dto.LoginRequest;
import xyz.qvtrmx.ai.auth.dto.RegisterRequest;
import xyz.qvtrmx.ai.common.api.ApiResponse;
import xyz.qvtrmx.ai.common.exception.BusinessException;
import xyz.qvtrmx.ai.security.model.AuthenticatedUser;

@Aspect
@Component
public class SensitiveOperationAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(SensitiveOperationAspect.class);

    @Around("@annotation(operation)")
    public Object audit(ProceedingJoinPoint joinPoint, SensitiveOperation operation) throws Throwable {
        AuditContext context = AuditContext.from(joinPoint.getArgs());
        try {
            Object result = joinPoint.proceed();
            LOGGER.info("audit_event={} outcome=SUCCEEDED{}{}", operation.value(), context, successDetails(result));
            return result;
        } catch (BusinessException exception) {
            LOGGER.warn("audit_event={} outcome=FAILED status={} reason={}{}", operation.value(),
                    exception.status().value(), exception.getMessage(), context);
            throw exception;
        } catch (Throwable exception) {
            LOGGER.error("audit_event={} outcome=FAILED reason=unexpected_error{}", operation.value(), context, exception);
            throw exception;
        }
    }

    private String successDetails(Object result) {
        if (!(result instanceof ApiResponse<?> response)) {
            return "";
        }
        if (response.data() instanceof RechargeUserResponse recharge) {
            return " balanceAfter=" + recharge.balance();
        }
        if (response.data() instanceof BalanceAdjustmentResponse refund) {
            return " balanceAfter=" + refund.balance();
        }
        return "";
    }

    private record AuditContext(
            Long actorId,
            String actorUsername,
            Long targetUserId,
            String username,
            BigDecimal amount,
            Boolean enabled,
            String clientIp
    ) {
        private static AuditContext from(Object[] arguments) {
            Long actorId = null;
            String actorUsername = null;
            Long targetUserId = null;
            String username = null;
            BigDecimal amount = null;
            Boolean enabled = null;
            String clientIp = null;

            for (Object argument : arguments) {
                if (argument instanceof AuthenticatedUser user) {
                    actorId = user.id();
                    actorUsername = user.username();
                } else if (argument instanceof Long userId) {
                    targetUserId = userId;
                } else if (argument instanceof LoginRequest request) {
                    username = request.username();
                } else if (argument instanceof RegisterRequest request) {
                    username = request.username();
                } else if (argument instanceof RechargeUserRequest request) {
                    amount = request.amount();
                } else if (argument instanceof RefundUserRequest request) {
                    amount = request.amount();
                } else if (argument instanceof UpdateUserStatusRequest request) {
                    enabled = request.enabled();
                } else if (argument instanceof HttpServletRequest request) {
                    clientIp = request.getRemoteAddr();
                }
            }
            return new AuditContext(actorId, actorUsername, targetUserId, username, amount, enabled, clientIp);
        }

        @Override
        public String toString() {
            StringBuilder values = new StringBuilder();
            append(values, "actorId", actorId);
            append(values, "actorUsername", actorUsername);
            append(values, "targetUserId", targetUserId);
            append(values, "username", username);
            append(values, "amount", amount);
            append(values, "enabled", enabled);
            append(values, "clientIp", clientIp);
            return values.toString();
        }

        private static void append(StringBuilder values, String key, Object value) {
            if (value != null) {
                values.append(' ').append(key).append('=').append(value);
            }
        }
    }
}
