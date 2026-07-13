package xyz.qvtrmx.ai.admin.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import xyz.qvtrmx.ai.user.entity.User;
import xyz.qvtrmx.ai.user.mapper.UserMapper;
import xyz.qvtrmx.ai.security.model.UserRole;

@Configuration
public class InitialAdminConfiguration {

    private static final int ADMIN_ROLE = UserRole.ADMIN.code();
    private static final int ENABLED_STATUS = 1;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,32}$");

    @Bean
    @ConditionalOnProperty(name = "app.admin.bootstrap.enabled", havingValue = "true")
    public ApplicationRunner initialAdminRunner(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.bootstrap.username}") String username,
            @Value("${app.admin.bootstrap.password}") String password,
            @Value("${app.admin.bootstrap.nickname}") String nickname
    ) {
        return arguments -> {
            validateBootstrapCredentials(username, password);
            if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, username))) {
                return;
            }

            User admin = new User();
            admin.setUsername(username);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setNickname(nickname);
            admin.setBalance(BigDecimal.ZERO);
            admin.setRole(ADMIN_ROLE);
            admin.setStatus(ENABLED_STATUS);
            admin.setVersion(0);
            admin.setDeleted(0);
            userMapper.insert(admin);
        };
    }

    private void validateBootstrapCredentials(String username, String password) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalStateException("INITIAL_ADMIN_USERNAME must be 4-32 letters, numbers, or underscores");
        }
        if (password.length() < 8 || password.length() > 72) {
            throw new IllegalStateException("INITIAL_ADMIN_PASSWORD must be 8-72 characters");
        }
    }
}
