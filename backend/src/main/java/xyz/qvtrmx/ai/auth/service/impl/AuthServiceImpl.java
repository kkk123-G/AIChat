package xyz.qvtrmx.ai.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import xyz.qvtrmx.ai.auth.dto.LoginRequest;
import xyz.qvtrmx.ai.auth.dto.RegisterRequest;
import xyz.qvtrmx.ai.auth.entity.RefreshTokenSession;
import xyz.qvtrmx.ai.auth.mapper.RefreshTokenSessionMapper;
import xyz.qvtrmx.ai.auth.service.AuthService;
import xyz.qvtrmx.ai.auth.vo.TokenResponse;
import xyz.qvtrmx.ai.common.exception.BusinessException;
import xyz.qvtrmx.ai.security.jwt.JwtService;
import xyz.qvtrmx.ai.security.model.AuthenticatedUser;
import xyz.qvtrmx.ai.security.model.UserRole;
import xyz.qvtrmx.ai.user.entity.User;
import xyz.qvtrmx.ai.user.mapper.UserMapper;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int USER_ROLE = UserRole.USER.code();
    private static final int ENABLED_STATUS = 1;
    private static final String REGISTER_LIMIT_PREFIX = "auth:register:ip:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";

    private final UserMapper userMapper;
    private final RefreshTokenSessionMapper refreshTokenSessionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate stringRedisTemplate;
    private final Duration refreshTokenTtl;
    private final Duration registerWindow;
    private final long registerLimit;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(
            UserMapper userMapper,
            RefreshTokenSessionMapper refreshTokenSessionMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            StringRedisTemplate stringRedisTemplate,
            @Value("${app.jwt.refresh-token-ttl}") Duration refreshTokenTtl,
            @Value("${app.auth.register-window}") Duration registerWindow,
            @Value("${app.auth.register-limit}") long registerLimit
    ) {
        this.userMapper = userMapper;
        this.refreshTokenSessionMapper = refreshTokenSessionMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.refreshTokenTtl = refreshTokenTtl;
        this.registerWindow = registerWindow;
        this.registerLimit = registerLimit;
    }

    @Override
    @Transactional
    public void register(RegisterRequest request, HttpServletRequest httpRequest) {
        enforceRegistrationLimit(clientIp(httpRequest));
        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, request.username()))) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户名已被占用");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setBalance(BigDecimal.ZERO);
        user.setRole(USER_ROLE);
        user.setStatus(ENABLED_STATUS);
        user.setVersion(0);
        user.setDeleted(0);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户名已被占用");
        }
    }

    @Override
    @Transactional
    public AuthTokens login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() != ENABLED_STATUS) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "账号已被禁用");
        }
        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginAt(now);
        user.setLastActiveAt(now);
        user.setLastUsedAt(now);
        userMapper.updateById(user);
        return issueTokens(user, request.deviceId(), httpRequest);
    }

    @Override
    @Transactional
    public AuthTokens refresh(String refreshToken, HttpServletRequest httpRequest) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "刷新令牌不能为空");
        }
        String tokenId = tokenId(refreshToken);
        if (tokenId == null || Boolean.FALSE.equals(stringRedisTemplate.hasKey(refreshCacheKey(tokenId)))) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "刷新令牌无效或已过期");
        }

        String tokenHash = sha256(refreshToken);
        RefreshTokenSession session = refreshTokenSessionMapper.selectOne(
                new LambdaQueryWrapper<RefreshTokenSession>().eq(RefreshTokenSession::getTokenHash, tokenHash)
        );
        if (session == null || session.getRevokedAt() != null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            stringRedisTemplate.delete(refreshCacheKey(tokenId));
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "刷新令牌无效或已过期");
        }

        int revoked = refreshTokenSessionMapper.update(
                new LambdaUpdateWrapper<RefreshTokenSession>()
                        .eq(RefreshTokenSession::getId, session.getId())
                        .isNull(RefreshTokenSession::getRevokedAt)
                        .set(RefreshTokenSession::getRevokedAt, LocalDateTime.now())
                        .set(RefreshTokenSession::getLastUsedAt, LocalDateTime.now())
        );
        if (revoked != 1) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "刷新令牌已被使用");
        }
        stringRedisTemplate.delete(refreshCacheKey(tokenId));

        User user = userMapper.selectById(session.getUserId());
        if (user == null || user.getStatus() != ENABLED_STATUS || user.getDeleted() == 1) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号不可用");
        }
        return issueTokens(user, session.getDeviceId(), httpRequest);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        String tokenId = tokenId(refreshToken);
        if (tokenId == null) {
            return;
        }
        String hash = sha256(refreshToken);
        refreshTokenSessionMapper.update(
                new LambdaUpdateWrapper<RefreshTokenSession>()
                        .eq(RefreshTokenSession::getTokenHash, hash)
                        .isNull(RefreshTokenSession::getRevokedAt)
                        .set(RefreshTokenSession::getRevokedAt, LocalDateTime.now())
        );
        stringRedisTemplate.delete(refreshCacheKey(tokenId));
    }

    private AuthTokens issueTokens(User user, String deviceId, HttpServletRequest request) {
        String tokenId = UUID.randomUUID().toString();
        String refreshToken = tokenId + "." + randomTokenValue();
        RefreshTokenSession session = new RefreshTokenSession();
        session.setUserId(user.getId());
        session.setTokenId(tokenId);
        session.setTokenHash(sha256(refreshToken));
        session.setDeviceId(deviceId);
        session.setUserAgent(trim(request.getHeader("User-Agent"), 512));
        session.setClientIp(clientIp(request));
        session.setExpiresAt(LocalDateTime.now().plus(refreshTokenTtl));
        refreshTokenSessionMapper.insert(session);
        stringRedisTemplate.opsForValue().set(refreshCacheKey(tokenId), session.getTokenHash(), refreshTokenTtl);

        String accessToken = jwtService.createAccessToken(new AuthenticatedUser(user.getId(), user.getUsername(), user.getRole()));
        return new AuthTokens(new TokenResponse(accessToken, "Bearer", jwtService.accessTokenExpiresInSeconds()), refreshToken, refreshTokenTtl);
    }

    private void enforceRegistrationLimit(String clientIp) {
        String key = REGISTER_LIMIT_PREFIX + clientIp;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, registerWindow);
        }
        if (count != null && count > registerLimit) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "注册请求过于频繁，请稍后再试");
        }
    }

    private String randomTokenValue() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String tokenId(String refreshToken) {
        if (refreshToken == null) {
            return null;
        }
        int separator = refreshToken.indexOf('.');
        if (separator <= 0 || separator == refreshToken.length() - 1) {
            return null;
        }
        try {
            return UUID.fromString(refreshToken.substring(0, separator)).toString();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String refreshCacheKey(String tokenId) {
        return REFRESH_TOKEN_PREFIX + tokenId;
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String trim(String value, int maxLength) {
        return value == null ? null : value.substring(0, Math.min(value.length(), maxLength));
    }
}
