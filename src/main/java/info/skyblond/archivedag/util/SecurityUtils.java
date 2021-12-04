package info.skyblond.archivedag.util;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public class SecurityUtils {
    public static final List<String> VALIDATE_ROLES = List.of(
            "ROLE_USER", "ROLE_VIEWER", "ROLE_UPLOADER", "ROLE_ADMIN"
    );

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

    public static void requireValidateRole(String role) {
        if (!VALIDATE_ROLES.contains(role)) {
            throw new IllegalArgumentException("Sort properties not in range.");
        }
    }

    public static void requireSortPropertiesInRange(Pageable pageable, List<String> range) {
        if (!pageable.getSort().stream().allMatch(o -> range.contains(o.getProperty()))) {
            throw new IllegalArgumentException("Sort properties not in range.");
        }
    }
}
