package xyz.qvtrmx.ai.auth.vo;

import java.util.List;

public record CurrentUserResponse(Long id, String username, int role, List<String> menuCodes) {
}
