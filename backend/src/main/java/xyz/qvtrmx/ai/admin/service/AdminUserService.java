package xyz.qvtrmx.ai.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import xyz.qvtrmx.ai.admin.vo.AdminUserResponse;
import xyz.qvtrmx.ai.common.api.PageResponse;
import xyz.qvtrmx.ai.common.exception.BusinessException;
import xyz.qvtrmx.ai.security.model.UserRole;
import xyz.qvtrmx.ai.user.entity.User;
import xyz.qvtrmx.ai.user.mapper.UserMapper;

@Service
public class AdminUserService {

    private static final List<Long> ALLOWED_PAGE_SIZES = List.of(10L, 20L, 50L);
    private static final int ENABLED_STATUS = 1;

    private final UserMapper userMapper;

    public AdminUserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public PageResponse<AdminUserResponse> listUsers(long pageNumber, long pageSize, String keyword) {
        if (pageNumber < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Page number must be greater than zero");
        }
        if (!ALLOWED_PAGE_SIZES.contains(pageSize)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Page size must be one of: 10, 20, 50");
        }

        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreatedAt);
        if (StringUtils.hasText(keyword)) {
            query.like(User::getUsername, keyword.trim());
        }

        Page<User> page = userMapper.selectPage(new Page<>(pageNumber, pageSize), query);
        List<AdminUserResponse> records = page.getRecords().stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                String.valueOf(user.getId()),
                user.getUsername(),
                UserRole.fromCode(user.getRole()).name(),
                user.getBalance(),
                user.getStatus() == ENABLED_STATUS ? "ENABLED" : "DISABLED",
                user.getLastActiveAt(),
                user.getLastUsedAt(),
                user.getCreatedAt()
        );
    }
}
