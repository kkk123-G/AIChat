package xyz.qvtrmx.ai.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import xyz.qvtrmx.ai.security.model.AuthenticatedUser;

class MenuPermissionServiceTest {

    private final MenuPermissionService menuPermissionService = new MenuPermissionService();

    @Test
    void ordinaryUserCanAccessOnlyAccountMenus() {
        List<String> menuCodes = menuPermissionService.accessibleMenuCodes(new AuthenticatedUser(1L, "user", 0));

        assertEquals(List.of(
                MenuPermissionService.AI_CHAT,
                MenuPermissionService.BALANCE_CHANGES,
                MenuPermissionService.USER_PROFILE
        ), menuCodes);
    }

    @Test
    void administratorCanAccessEveryMenu() {
        List<String> menuCodes = menuPermissionService.accessibleMenuCodes(new AuthenticatedUser(1L, "admin", 1));

        assertEquals(List.of(
                MenuPermissionService.ADMIN_DASHBOARD,
                MenuPermissionService.USER_MANAGEMENT,
                MenuPermissionService.RECHARGE_RECORDS,
                MenuPermissionService.AI_CHAT,
                MenuPermissionService.BALANCE_CHANGES,
                MenuPermissionService.USER_PROFILE
        ), menuCodes);
    }
}
