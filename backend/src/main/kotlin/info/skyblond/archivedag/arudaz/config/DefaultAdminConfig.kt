package info.skyblond.archivedag.arudaz.config

import info.skyblond.archivedag.arstue.UserManagementService
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
class DefaultAdminConfig(
    private val userManagementService: UserManagementService
) {
    @PostConstruct
    fun createDefaultAdminAccount() {
        if (!userManagementService.userExists("admin")) {
            userManagementService.createUser("admin", "admin")
        }
        val adminAccount = userManagementService.queryUser("admin")
        // here we don't change the account status
        if (!adminAccount.roles.contains("ROLE_ADMIN")) {
            userManagementService.addRoleToUser("admin", "ROLE_ADMIN")
        }
        if (!adminAccount.roles.contains("ROLE_USER")) {
            userManagementService.addRoleToUser("admin", "ROLE_USER")
        }
    }
}
