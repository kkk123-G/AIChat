package xyz.qvtrmx.ai.security.model;

public enum UserRole {
    USER(0, "ROLE_USER"),
    ADMIN(1, "ROLE_ADMIN");

    private final int code;
    private final String authority;

    UserRole(int code, String authority) {
        this.code = code;
        this.authority = authority;
    }

    public int code() {
        return code;
    }

    public String authority() {
        return authority;
    }

    public static UserRole fromCode(int code) {
        return switch (code) {
            case 0 -> USER;
            case 1 -> ADMIN;
            default -> throw new IllegalArgumentException("Unsupported user role: " + code);
        };
    }
}
