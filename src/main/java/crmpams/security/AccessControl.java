package crmpams.security;

import crmpams.model.CurrentUser;
import crmpams.model.Role;
import java.util.Arrays;

/**
 * Role-based access check used at the start of every service method.
 * Hiding a menu item is not enough, so the service layer checks the role again.
 */
public final class AccessControl {

    private AccessControl() {
    }

    public static void require(CurrentUser user, Role... allowed) {
        if (user == null) {
            throw new AccessDeniedException("Please log in first.");
        }
        if (Arrays.stream(allowed).noneMatch(r -> r == user.role())) {
            throw new AccessDeniedException("Your role (" + user.role().label()
                    + ") is not allowed to use this function.");
        }
    }
}
