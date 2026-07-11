package xyz.qvtrmx.ai.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import xyz.qvtrmx.ai.auth.dto.LoginRequest;
import xyz.qvtrmx.ai.auth.dto.RegisterRequest;
import xyz.qvtrmx.ai.auth.vo.TokenResponse;

public interface AuthService {

    void register(RegisterRequest request, HttpServletRequest httpRequest);

    AuthTokens login(LoginRequest request, HttpServletRequest httpRequest);

    AuthTokens refresh(String refreshToken, HttpServletRequest httpRequest);

    void logout(String refreshToken);

    record AuthTokens(TokenResponse response, String refreshToken, Duration refreshTokenTtl) {
    }
}
