package info.skyblond.archivedag.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {
    public static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static String getCurrentUsername() {
        return getCurrentAuthentication().getName();
    }

    public static boolean checkCurrentUserHasRole(String role) {
        Authentication auth = getCurrentAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}
