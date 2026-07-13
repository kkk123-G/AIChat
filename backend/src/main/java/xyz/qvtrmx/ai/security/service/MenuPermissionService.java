package xyz.qvtrmx.ai.security.service;

import java.util.List;
import org.springframework.stereotype.Service;
import xyz.qvtrmx.ai.security.model.AuthenticatedUser;
import xyz.qvtrmx.ai.security.model.UserRole;

@Service
public class MenuPermissionService {

    public static final String ADMIN_DASHBOARD = "admin-dashboard";
    public static final String USER_MANAGEMENT = "user-management";
    public static final String USAGE_RECORDS = "usage-records";
    public static final String USER_DASHBOARD = "user-dashboard";
    public static final String AI_CHAT = "ai-chat";
    public static final String USER_PROFILE = "user-profile";

    private static final List<String> USER_MENUS = List.of(
            USER_DASHBOARD,
            AI_CHAT,
            USER_PROFILE
    );
    private static final List<String> ADMIN_MENUS = List.of(
            ADMIN_DASHBOARD,
            USER_MANAGEMENT,
            USAGE_RECORDS,
            USER_DASHBOARD,
            AI_CHAT,
            USER_PROFILE
    );

    public List<String> accessibleMenuCodes(AuthenticatedUser user) {
        return UserRole.fromCode(user.role()) == UserRole.ADMIN ? ADMIN_MENUS : USER_MENUS;
    }
}
