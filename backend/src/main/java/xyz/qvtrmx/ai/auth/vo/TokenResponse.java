package xyz.qvtrmx.ai.auth.vo;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
