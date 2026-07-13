package xyz.qvtrmx.ai.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.factory.annotation.Value;
import xyz.qvtrmx.ai.auth.dto.LoginRequest;
import xyz.qvtrmx.ai.auth.dto.RefreshTokenRequest;
import xyz.qvtrmx.ai.auth.dto.RegisterRequest;
import xyz.qvtrmx.ai.auth.service.AuthService;
import xyz.qvtrmx.ai.auth.vo.CurrentUserResponse;
import xyz.qvtrmx.ai.auth.vo.TokenResponse;
import xyz.qvtrmx.ai.common.api.ApiResponse;
import xyz.qvtrmx.ai.security.model.AuthenticatedUser;
import xyz.qvtrmx.ai.security.service.MenuPermissionService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private final AuthService authService;
    private final MenuPermissionService menuPermissionService;
    private final boolean refreshCookieSecure;

    public AuthController(
            AuthService authService,
            MenuPermissionService menuPermissionService,
            @Value("${app.auth.refresh-cookie-secure}") boolean refreshCookieSecure
    ) {
        this.authService = authService;
        this.menuPermissionService = menuPermissionService;
        this.refreshCookieSecure = refreshCookieSecure;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        authService.register(request, httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.ok(null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthService.AuthTokens tokens = authService.login(request, httpRequest);
        return withRefreshCookie(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@RequestBody(required = false) RefreshTokenRequest request,
                                                               HttpServletRequest httpRequest) {
        String refreshToken = request != null && request.refreshToken() != null
                ? request.refreshToken()
                : refreshTokenFromCookie(httpRequest);
        AuthService.AuthTokens tokens = authService.refresh(refreshToken, httpRequest);
        return withRefreshCookie(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) RefreshTokenRequest request,
                                                     HttpServletRequest httpRequest) {
        String refreshToken = request != null && request.refreshToken() != null
                ? request.refreshToken()
                : refreshTokenFromCookie(httpRequest);
        authService.logout(refreshToken);
        ResponseCookie cookie = refreshCookie("", Duration.ZERO);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(ApiResponse.ok(null));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(new CurrentUserResponse(
                user.id(),
                user.username(),
                user.role(),
                menuPermissionService.accessibleMenuCodes(user)
        ));
    }

    private ResponseEntity<ApiResponse<TokenResponse>> withRefreshCookie(AuthService.AuthTokens tokens) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, tokens.refreshToken())
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(tokens.refreshTokenTtl())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(tokens.response()));
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }

    private String refreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
