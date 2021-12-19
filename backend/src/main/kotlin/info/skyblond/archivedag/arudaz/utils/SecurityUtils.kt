package info.skyblond.archivedag.arudaz.utils

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

val validatedRoles = listOf(
    "ROLE_USER", "ROLE_VIEWER", "ROLE_UPLOADER", "ROLE_ADMIN"
)

fun getCurrentAuthentication(): Authentication? {
    return SecurityContextHolder.getContext().authentication
}

fun getCurrentUsername(): String {
    return getCurrentAuthentication()!!.name
}

fun checkCurrentUserIsAdmin(): Boolean {
    val auth = getCurrentAuthentication() ?: return false
    return auth.authorities.stream().anyMatch { a: GrantedAuthority -> a.authority == "ROLE_ADMIN" }
}

fun requireValidateRole(role: String) {
    require(validatedRoles.contains(role)) { "Invalid role. Role can only be one of: $validatedRoles" }
}

fun requireSortPropertiesInRange(pageable: Pageable, range: List<String>) {
    require(pageable.sort.stream().allMatch { o: Sort.Order ->
        range.contains(
            o.property
        )
    }) { "Sort properties not in range." }
}
